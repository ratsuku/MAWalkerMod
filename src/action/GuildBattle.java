package action;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GuildBattle {
	public static final Action Name = Action.GUILD_BATTLE;
	private static byte[] response;

	public static boolean run() throws Exception {
		// check tickets
		if (Process.info.ticket <= 0) {
			return false;
		}

		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("guild_id", Process.info.fairy.guildId));
		post.add(new BasicNameValuePair("no", Process.info.fairy.deckNo));
		post.add(new BasicNameValuePair("serial_id",
				Process.info.fairy.serialId));
		Process.info.fairy.getClass();
		post.add(new BasicNameValuePair("spp_skill_serial", "dummy"));
		try {
			response = Process.network
					.ConnectToServer(
							"http://web.million-arthurs.com/connect/app/fairy/guild_fairy_battle?cyt=1",
							post, false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getLocalizedMessage();
			throw ex;
		}
		Document doc;
		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			if (GuildDefeat.judge(doc)) {
				Process.info.events.push(Info.EventType.guildTopRetry);
				return true;
			}

			ParseUserDataInfo.parse(doc);

			if (xpath.evaluate("//own_fairy_battle_result/winner", doc).equals(
					"1"))
				Process.info.events.push(Info.EventType.fairyBattleWin);
			else {
				Process.info.events.push(Info.EventType.fairyBattleLose);
			}

			Process.info.week = xpath
					.evaluate("//week_total_contribution", doc);

			Process.info.SetTimeoutByAction(Name);
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;
	}
}
