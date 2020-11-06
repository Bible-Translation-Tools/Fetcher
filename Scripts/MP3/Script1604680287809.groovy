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

WebUI.openBrowser('')

WebUI.navigateToUrl('https://audio.bibleineverylanguage.org/')

WebUI.click(findTestObject('Page_Landing/span_Get Started'))

WebUI.click(findTestObject('Object Repository/Page_Languages/p_English'))

WebUI.click(findTestObject('Object Repository/Page_File Types/p_MP3 File'))

WebUI.click(findTestObject('Page_Books/p_Titus'))

WebUI.click(findTestObject('Object Repository/Page_Chapters/p_Download'))

WebUI.delay(6)

String home = System.getProperty("user.home")

String userDownloadPath = new File(home + '/Downloads/')

CustomKeywords.'customKeywords.file.isFileDownloaded'(userDownloadPath, 'en_nt_ulb_tit_c01.mp3')

WebUI.delay(3)

WebUI.closeBrowser()

