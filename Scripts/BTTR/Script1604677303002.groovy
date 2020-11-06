import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.entity.StringEntity
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities

import com.fasterxml.jackson.databind.ObjectMapper
import com.kms.katalon.core.webui.driver.DriverFactory

WebUI.openBrowser('')

WebUI.navigateToUrl('https://audio.bibleineverylanguage.org/')

WebUI.click(findTestObject('Object Repository/Page_Landing/span_Get Started'))

WebUI.click(findTestObject('Object Repository/Page_Languages/a_en                English        English _75e531'))

WebUI.click(findTestObject('Object Repository/Page_File Types/p_BTT Recorder'))

WebUI.click(findTestObject('Object Repository/Page_Books/p_Titus'))

WebUI.click(findTestObject('Object Repository/Page_Chapters/p_Download Book'))

WebUI.delay(6)

String home = System.getProperty("user.home")

String userDownloadPath = new File(home + '/Downloads/')

HashMap<String, Object> chromePreferences = new HashMap<String, Object>();
chromePreferences.put("profile.default_content_settings.popups", 0);
chromePreferences.put("download.prompt_for_download", "false");
chromePreferences.put("download.default_directory", userDownloadPath);
ChromeOptions chromeOptions = new ChromeOptions();
System.setProperty("webdriver.chrome.driver", DriverFactory.getChromeDriverPath())

chromeOptions.addArguments("start-maximized");
chromeOptions.addArguments("disable-infobars");

//HEADLESS CHROME
chromeOptions.addArguments("headless");

chromeOptions.setExperimentalOption("prefs", chromePreferences);
DesiredCapabilities cap = DesiredCapabilities.chrome();
cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
cap.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

ChromeDriverService driverService = ChromeDriverService.createDefaultService();
ChromeDriver driver = new ChromeDriver(driverService, chromeOptions);

Map<String, Object> commandParams = new HashMap<>();
commandParams.put("cmd", "Page.setDownloadBehavior");
Map<String, String> params = new HashMap<>();
params.put("behavior", "allow");
params.put("downloadPath", userDownloadPath);
commandParams.put("params", params);
ObjectMapper objectMapper = new ObjectMapper();
HttpClient httpClient = HttpClientBuilder.create().build();
String command = objectMapper.writeValueAsString(commandParams);
String u = driverService.getUrl().toString() + "/session/" + driver.getSessionId() + "/chromium/send_command";
HttpPost request = new HttpPost(u);
request.addHeader("content-type", "application/json");
request.setEntity(new StringEntity(command));
try {
	httpClient.execute(request);
	driver.get("https://audio-content.bibleineverylanguage.org/en/ulb/tit/CONTENTS/tr/mp3/low/verse/en_ulb_tit.tr");
	WebUI.delay(30)
	System.out.println("Task complete, please go to save folder to see it.");
	driver.close()
} catch (IOException e2) {
	e2.printStackTrace();
}

CustomKeywords.'customKeywords.file.isFileDownloaded'(userDownloadPath, 'en_ulb_tit.tr')

WebUI.delay(3)

WebUI.closeBrowser()


