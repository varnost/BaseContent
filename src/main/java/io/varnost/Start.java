package io.varnost.baseContent;

import io.varnost.corengine.Alert;
import io.varnost.corengine.LogStream;
import io.varnost.corengine.RuleInterface;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Start {
    public static void main(String[] args) throws Exception {

        // Initialize Stream Execution Env and get the flow of logs
        LogStream logStream = new LogStream();
        DataStream<ObjectNode> logs = logStream.openStream();

        // Create a new List of Streams, one for each "rule" that is being executed
        List<RuleInterface> rules = Arrays.asList(
                new FourOhFourBySource(),
                new TwoHundy(),
                new IRC_C2_Traffic_Pattern()
        );
        List<DataStream<Alert>> outputs = new ArrayList<>();
        for (RuleInterface rule : rules) {
            outputs.add(rule.logic(logs));
        }
        // Join the outputs of each stream together into one for output

        Optional<DataStream<Alert>> alerts;
        if (outputs.size() > 1)
            alerts = outputs.stream().reduce(DataStream::union);
        else
            alerts = Optional.ofNullable(outputs.get(0));

        // Ensure there was no issue re-combining streams and end the pipeline
        if (alerts.isPresent()) {
            logStream.closeStream(alerts.get());
        }
    }
}
