package com.nouhaila.docmind;

import com.jayway.jsonpath.JsonPath;
import com.nouhaila.docmind.config.RabbitConfig;
import com.nouhaila.docmind.document.DocumentRepository;
import com.nouhaila.docmind.document.DocumentStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.ai.google.genai.api-key=test-key",
        "app.storage.dir=target/test-uploads"
})
@Testcontainers
class DocmindIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");

    // Test doubles: no ONNX model, no pgvector table, no Gemini call in these tests.
    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    VectorStore vectorStore;

    @Autowired
    WebApplicationContext context;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    AmqpAdmin amqpAdmin;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    // ---------- helpers ----------

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String newEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.token");
    }

    private String registerAndLogin() throws Exception {
        return registerAndLogin(newEmail());
    }

    private long upload(String token, String filename, byte[] bytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/pdf", bytes);
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn().getResponse().getContentAsString();

        Number id = JsonPath.read(body, "$.id");
        return id.longValue();
    }

    private static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            pdf.save(out);
            return out.toByteArray();
        }
    }

    // ---------- tests ----------

    @Test
    void registrationLoginAndProtectedEndpointWork() throws Exception {
        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isCreated());

        // the same email twice is refused
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isConflict());

        // a too-short password is refused by validation
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(newEmail(), "123")))
                .andExpect(status().isBadRequest());

        // a wrong password is refused
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "wrong-password")))
                .andExpect(status().isUnauthorized());

        // protected endpoint: no token -> 401, valid token -> 200
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello " + email));

        // a forged token is refused
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadedPdfIsProcessedInTheBackgroundThroughRabbitMq() throws Exception {
        String token = registerAndLogin();

        long id = upload(token, "hello.pdf", pdfWithText("Hello from the integration test"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(documentRepository.findById(id).orElseThrow().getStatus())
                        .isEqualTo(DocumentStatus.READY));

        verify(vectorStore, timeout(10_000)).add(anyList());
    }

    @Test
    void brokenPdfEndsAsFailedAndIsDeadLettered() throws Exception {
        String token = registerAndLogin();

        long id = upload(token, "fake.pdf", "this is not a pdf".getBytes());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(documentRepository.findById(id).orElseThrow().getStatus())
                        .isEqualTo(DocumentStatus.FAILED));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            QueueInformation dlq = amqpAdmin.getQueueInfo(RabbitConfig.DEAD_LETTER_QUEUE);
            assertThat(dlq).isNotNull();
            assertThat(dlq.getMessageCount()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void usersCannotSeeOrReprocessEachOthersDocuments() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        long id = upload(tokenA, "private.pdf", pdfWithText("Private content"));

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/documents/" + id + "/process").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void nonPdfFilesAreRejected() throws Exception {
        String token = registerAndLogin();
        MockMultipartFile notes = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(notes)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askWithoutRelevantDocumentsAnswersNotFoundWithoutCallingTheModel() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What are my strengths?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("I couldn't find this in your documents."))
                .andExpect(jsonPath("$.sources").isEmpty());
    }
}