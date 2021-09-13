package com.uci.utils.telemetry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.json.JSONObject;

@Plugin(name = "CustomLogLayout", category = "Core", elementType = "layout", printObject = true)
public class LogTelemetryLayout  extends AbstractStringLayout{
    protected LogTelemetryLayout( Charset charset ){
        super( charset );
    }

    @Override
    public String toSerializable( LogEvent event ) {
    	String message = event.getMessage().getFormattedMessage();
    	LogTelemetryBuilder object = new LogTelemetryBuilder();
 		
 		String msgBody = object.build(message, null, "", "", "", "", "", "", "");
 		JSONObject jsonBody = new JSONObject(msgBody);
 		
 		return jsonBody.toString();
    }

    @PluginFactory
    public static LogTelemetryLayout createLayout(@PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset) {
        return new LogTelemetryLayout(charset);
    }
}
