package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import es.unizar.urlshortener.core.usecases.ShowShortUrlInfoUseCase
import es.unizar.urlshortener.core.usecases.GetQRUseCaseImpl
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
import org.assertj.core.api.Assertions.assertThatCode
import org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito

@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(
    classes = [
        UserAgentInfoImpl::class,
        IsReachableServiceImpl::class,
        SafeBrowsingServiceImpl::class,
        HashServiceImpl::class]
)
class FunctionalitiesTest {

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
    private lateinit var userAgentInfo: UserAgentInfo
    
    @MockBean
    private lateinit var showShortUrlInfoUseCase: ShowShortUrlInfoUseCase

    @MockBean
    private lateinit var hashService: HashServiceImpl

    @Test
    fun `test UserAgentInfo service for Windows and Firefox`() {
        val x =   UserAgentInfoImpl()
        assertEquals("Windows", x.getOS("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0"))
        assertEquals("Firefox-12.0", x.getBrowser("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0"))
    } 
    
    @Test
    fun `test UserAgentInfo service for Unix and Chrome`() {
        val x =   UserAgentInfoImpl()
        assertEquals("Unix", x.getOS("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"))
        assertEquals("Chrome-98.0.4758.102", x.getBrowser("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"))
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

    @Test
    fun `test customUrl service`() {
        assertEquals("patata", hashService.hasUrl("https://www.example.com", "patata"))
        assertEquals("6b30f676", hashService.hasUrl("https://www.example.com", ""))
    }
    

}