package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import GenerateQRUseCase
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

@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        RestResponseEntityExceptionHandler::class,
        IsReachableServiceImpl::class,
        SafeBrowsingServiceImpl::class,
        MessageBrokerImpl::class]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var subject:  MessageBrokerImpl 
    @MockBean
    private lateinit var rabbitTemplateMock: RabbitTemplate

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

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var shortUrlRepository: ShortUrlRepositoryService

    @MockBean
    private lateinit var generateQRUseCase: GenerateQRUseCase

    @MockBean
    private lateinit var userAgentInfo: UserAgetInfo
    
    
   /*  @Test
    fun testSend() {
       // rabbitTemplateMock = Mockito.mock(RabbitTemplate)
        subject = MessageBrokerImpl(shortUrlRepository)
        //given(subject.sendSafeBrowsing("safeBrowsing","Test sendSafeBrowsing","aaaaaaa")).doesNotThrowAnyException()
        assertAll(subject.sendSafeBrowsing("safeBrowsing","Test sendSafeBrowsing","aaaaaaa"))
        Mockito.verify(rabbitTemplateMock)
            .convertAndSend(eq("safeBrowsing"), eq("Test sendSafeBrowsing"),eq("aaaaaaa"))
    }*/
    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))
        given(userAgentInfo.getBrowser("key")).willReturn("a")
        given(userAgentInfo.getOS("key")).willReturn("b")
        given(shortUrlRepository.isSafe("key")).willReturn(true)
        given(shortUrlRepository.isReachable("key")).willReturn(true)

        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1", browser = "a",platform = "b"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }
         println("\n\n---------------------------------------El location es 2:\n\n")
        mockMvc.perform(get("/{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
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
        given(shortUrlRepository.isSafe("f684a3c4")).willReturn(false)
        given(shortUrlRepository.isReachable("f684a3c4")).willReturn(true)

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
    @Test
    fun `test SafeBrowsing service`() {
        assertEquals(true, safeBrowsingService.isSafe("https://example.com/"))
        assertEquals(false, safeBrowsingService.isSafe("https://testsafebrowsing.appspot.com/s/phishing.html"))
    }
    @Test
    fun `test Reachablilty service`() {
        assertEquals(true, isReachableService.isReachable("http://unizar.es"))
        assertEquals(false, isReachableService.isReachable("http://unizr.es"))
    }

    

}