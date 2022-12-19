package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import es.unizar.urlshortener.core.usecases.ShowShortUrlInfoUseCase
import es.unizar.urlshortener.core.usecases.GetQRUseCase
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.*
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.Mockito.mock
import org.springframework.test.web.servlet.get
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.amqp.rabbit.core.RabbitTemplate
import  org.assertj.core.api.Assertions.assertThatCode
import  org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito
import org.springframework.amqp.rabbit.core.RabbitAdmin
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class,
        IsReachableServiceImpl::class,
        SafeBrowsingServiceImpl::class,
        UserAgentInfoImpl::class,
        MessageBrokerImpl::class]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var subject:  MessageBrokerImpl 
    
    @Autowired
    @MockBean
    private lateinit var rabbitTemplateMock: RabbitTemplate
    
    @MockBean
    private lateinit var rabbitAdminMock: RabbitAdmin


    @Autowired
    private lateinit var mockMvc: MockMvc

   
    @Autowired
    private lateinit var isReachableService: IsReachableServiceImpl
    
    @Autowired
    private lateinit var safeBrowsingService:SafeBrowsingServiceImpl

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

   // @Autowired
    //private lateinit var messageBrokerTesting: MessageBrokerImpl

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var shortUrlRepository: ShortUrlRepositoryService

    @MockBean
    private lateinit var generateQRUseCase: GetQRUseCase

    @MockBean
    private lateinit var userAgentInfo: UserAgentInfo
    
    @MockBean
    private lateinit var showShortUrlInfoUseCase: ShowShortUrlInfoUseCase
    
    @MockBean
    private lateinit var hashService: HashServiceImpl

    fun testSend() {
       /* rabbitTemplateMock = Mockito.mock(RabbitTemplate)
        subject = MessageBrokerImpl(shortUrlRepository)
        //given(subject.sendSafeBrowsing("safeBrowsing","Test sendSafeBrowsing","aaaaaaa")).doesNotThrowAnyException()
        assertAll(subject.sendSafeBrowsing("safeBrowsing","Test sendSafeBrowsing","aaaaaaa"))
        Mockito.verify(rabbitTemplateMock)
            .convertAndSend(eq("safeBrowsing"), eq("Test sendSafeBrowsing"),eq("aaaaaaa"))*/
        val rabbitT= RabbitTemplate()
        
        val messageBrokerTest = MessageBrokerImpl(shortUrlRepository,safeBrowsingService,rabbitTemplateMock,isReachableService)
        
        rabbitAdminMock.purgeQueue("isReachable", true)
        messageBrokerTest.sendSafeBrowsing("myUrl","myHash")
        assertTrue(isMessagePublishedInQueue(rabbitTemplateMock))
    }

    private fun isMessagePublishedInQueue(a: RabbitTemplate): Boolean {
        //rabbitTemplateMock = RabbitTemplate()
        val queueMessageCount = rabbitTemplateMock.execute {
                it.queueDeclare(
                        "isReachable",
                        true,
                        false, 
                        false, 
                        null
                ).messageCount
        }
        val queuedMessage = rabbitTemplateMock
                .receiveAndConvert(
                        "isReachable"
                ) as String

        return queueMessageCount == 1 && queuedMessage.equals("myUrl myHash")
}
    
    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("idHash")).willReturn(Redirection("http://example.com/"))
        given(userAgentInfo.getBrowser("UrlAgentHeader")).willReturn("a")
        given(userAgentInfo.getOS("UrlAgentHeader")).willReturn("b")
        given(shortUrlRepository.everythingChecked("idHash")).willReturn(true)
        given(shortUrlRepository.isSafe("idHash")).willReturn(true)
        given(shortUrlRepository.isReachable("idHash")).willReturn(true)

        mockMvc.perform(get("/{id}", "idHash").header("User-Agent","UrlAgentHeader"))
            .andDo(print())
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("idHash", ClickProperties(ip = "127.0.0.1", browser = "a",platform = "b"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("idHash"))
            .willAnswer { throw RedirectionNotFound("idHash") }
         println("\n\n---------------------------------------El location es 2:\n\n")
        mockMvc.perform(get("/{id}", "idHash"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("idHash", ClickProperties(ip = "127.0.0.1"))
    }


    @Test
    fun `redirectTo returns a not validated yet when the key exists but is not validated`() {
        given(redirectUseCase.redirectTo("idHash")).willReturn(Redirection("http://example.com/"))
        given(userAgentInfo.getBrowser("UrlAgentHeader")).willReturn("a")
        given(userAgentInfo.getOS("UrlAgentHeader")).willReturn("b")
        given(shortUrlRepository.everythingChecked("idHash")).willReturn(false)

        mockMvc.perform(get("/{id}", "idHash").header("User-Agent","UrlAgentHeader"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))

        verify(logClickUseCase).logClick("idHash", ClickProperties(ip = "127.0.0.1", browser = "a",platform = "b"))
    }

    @Test
    fun `redirectTo returns a not safe when the key exists but is not safe`() {
        given(redirectUseCase.redirectTo("idHash")).willReturn(Redirection("http://example.com/"))
        given(userAgentInfo.getBrowser("UrlAgentHeader")).willReturn("a")
        given(userAgentInfo.getOS("UrlAgentHeader")).willReturn("b")
        given(shortUrlRepository.everythingChecked("idHash")).willReturn(true)
        given(shortUrlRepository.isSafe("idHash")).willReturn(false)

        mockMvc.perform(get("/{id}", "idHash").header("User-Agent","UrlAgentHeader"))
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.statusCode").value(403))

        verify(logClickUseCase).logClick("idHash", ClickProperties(ip = "127.0.0.1", browser = "a",platform = "b"))
    }

     @Test
    fun `redirectTo returns a not reachable when the key exists but is not reachable`() {
        given(redirectUseCase.redirectTo("idHash")).willReturn(Redirection("http://example.com/"))
        given(userAgentInfo.getBrowser("UrlAgentHeader")).willReturn("a")
        given(userAgentInfo.getOS("UrlAgentHeader")).willReturn("b")
        given(shortUrlRepository.everythingChecked("idHash")).willReturn(true)
        given(shortUrlRepository.isSafe("idHash")).willReturn(true)
        given(shortUrlRepository.isReachable("idHash")).willReturn(false)

        mockMvc.perform(get("/{id}", "idHash").header("User-Agent","UrlAgentHeader"))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))

        verify(logClickUseCase).logClick("idHash", ClickProperties(ip = "127.0.0.1", browser = "a",platform = "b"))
    }


     @Test
     fun `creates returns a basic redirect if it can compute a hash`() {
         given(
             createShortUrlUseCase.create(
                 url = "http://example.com/",
                 data = ShortUrlProperties(ip = "127.0.0.1"),
                 customUrl = "",
                 wantQR = false
             )
         ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
         given(shortUrlRepository.everythingChecked("f684a3c4")).willReturn(true)
        given(shortUrlRepository.isSafe("f684a3c4")).willReturn(true)
        given(shortUrlRepository.isReachable("f684a3c4")).willReturn(true)

         mockMvc.perform(
             post("/api/link")
                 .param("url", "http://example.com/")
                 .param("customUrl", "")
                 .param("wantQR","false")
                 .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
         )
             .andDo(print())
             .andExpect(status().isCreated)
             .andExpect(redirectedUrl("http://localhost/f684a3c4"))
             .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
     }

    @Test
     fun `creates returns Forbidden if it can compute a hash but is not safe`() {
         given(
             createShortUrlUseCase.create(
                 url = "http://example.com/",
                 data = ShortUrlProperties(ip = "127.0.0.1"),
                 customUrl = "",
                 wantQR = false
             )
         ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        given(shortUrlRepository.everythingChecked("f684a3c4")).willReturn(true)
        given(shortUrlRepository.isSafe("f684a3c4")).willReturn(false)

         mockMvc.perform(
             post("/api/link")
                 .param("url", "http://example.com/")
                 .param("customUrl", "")
                 .param("wantQR","false")
                 .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
         )
             .andDo(print())
             .andExpect(status().isForbidden)
             .andExpect(jsonPath("$.statusCode").value(403))
     }

     @Test
     fun `creates returns not safe error if it can compute a hash but is not safe`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customUrl = "",
                wantQR = false
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        given(shortUrlRepository.everythingChecked("f684a3c4")).willReturn(true)
        given(shortUrlRepository.isSafe("f684a3c4")).willReturn(true)
        given(shortUrlRepository.isReachable("f684a3c4")).willReturn(false)

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("customUrl", "")
                .param("wantQR","false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
     }

    @Test
     fun `creates returns error if it can compute a hash but is not validated yet`() {
         given(
             createShortUrlUseCase.create(
                 url = "http://example.com/",
                 data = ShortUrlProperties(ip = "127.0.0.1"),
                 customUrl = "",
                 wantQR = false
             )
         ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))
        given(shortUrlRepository.everythingChecked("f684a3c4")).willReturn(false)

         mockMvc.perform(
             post("/api/link")
                 .param("url", "http://example.com/")
                 .param("customUrl", "")
                 .param("wantQR","false")
                 .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
         )
             .andDo(print())
             .andExpect(status().isBadRequest)
             .andExpect(jsonPath("$.statusCode").value(400))
     }

     @Test
     fun `creates returns bad request if it can compute a hash`() {
         given(
             createShortUrlUseCase.create(
                 url = "ftp://example.com/",
                 data = ShortUrlProperties(ip = "127.0.0.1"),
                 customUrl = "",
                 wantQR = false
             )
         ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

         mockMvc.perform(
             post("/api/link")
                .param("url", "ftp://example.com/")
                .param("customUrl", "")
                .param("wantQR","false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
         )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }


}