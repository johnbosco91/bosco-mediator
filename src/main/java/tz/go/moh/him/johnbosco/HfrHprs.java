package tz.go.moh.him.johnbosco;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HfrHprs extends UntypedActor {
    private final MediatorConfig config;
    public HfrHprs(MediatorConfig configHprs) {
        this.config = configHprs;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {

            //Get request body
            String bodyData = ((MediatorHTTPRequest) msg).getBody();

            //Serialize the obtained string body
            SourceMessage SourceMessage = new Gson().fromJson(bodyData, SourceMessage.class);

            if(!StringUtils.isBlank(SourceMessage.getLatitude()) || !StringUtils.isBlank(SourceMessage.getLatitude())){
                try{
                    double latitude = Double.parseDouble(SourceMessage.getLatitude());
                    double longitude = Double.parseDouble(SourceMessage.getLongitude());
                }catch(Exception e){
                    FinishRequest finishRequest = new FinishRequest(
                            "Invalid latitude/longitude",
                            "application/json",
                            HttpStatus.SC_BAD_REQUEST);

                    ((MediatorHTTPRequest) msg).getRequestHandler().tell(finishRequest,
                            getSelf());
                    return;
                }

            }
            //Serialize object back to JSON
            String convertedMessage = new Gson().toJson(SourceMessage);

            HashMap<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");


            //post response to non console
            JSONObject connectionProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("hprs");
            String username = connectionProperties.getString("username");
            String password = connectionProperties.getString("password");

            String credentials = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(credentials.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            header.put("Authorization", authHeader);

            String uri = connectionProperties.getString("scheme") + connectionProperties.getString("host") + ":" + connectionProperties.getString("port") + connectionProperties.get("path");
            MediatorHTTPRequest hprsRequest =
                    new MediatorHTTPRequest(((MediatorHTTPRequest) msg).getRequestHandler(), getSelf(),
                            "Sending message from mediator to HPRS", "POST", uri,
                            convertedMessage, header, null);

            ActorSelection httpConnector = getContext().actorSelection(config.userPathFor("http-connector"));
            httpConnector.tell(hprsRequest, getSelf());
        }else if(msg instanceof MediatorHTTPResponse){
            ((MediatorHTTPResponse) msg).getOriginalRequest().getRequestHandler().tell(((MediatorHTTPResponse) msg).toFinishRequest(), getSelf());
        } else {
            unhandled(msg);
        }
    }
}
