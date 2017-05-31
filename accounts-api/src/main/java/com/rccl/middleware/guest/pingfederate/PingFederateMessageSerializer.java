package com.rccl.middleware.guest.pingfederate;

import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightbend.lagom.javadsl.api.deser.DeserializationException;
import com.lightbend.lagom.javadsl.api.deser.StrictMessageSerializer;
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol;
import com.lightbend.lagom.javadsl.api.transport.NotAcceptable;
import com.lightbend.lagom.javadsl.api.transport.UnsupportedMediaType;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PingFederateMessageSerializer implements StrictMessageSerializer<ReferenceId> {
    
    @Override
    public NegotiatedDeserializer<ReferenceId, ByteString> deserializer(MessageProtocol protocol) throws UnsupportedMediaType {
        if (protocol.contentType().isPresent()) {
            if (protocol.contentType().get().equals("text/html")) {
                return new ReferenceIdDeserializer();
            } else if (protocol.contentType().get().equals("application/json")) {
                return new ReferenceIdDeserializer();
            } else {
                throw new UnsupportedMediaType(protocol, new MessageProtocol().withContentType("text/html"));
            }
        } else {
            return new ReferenceIdDeserializer();
        }
    }
    
    static class ReferenceIdDeserializer implements NegotiatedDeserializer<ReferenceId, ByteString> {
        
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        
        @Override
        public ReferenceId deserialize(ByteString byteString) throws DeserializationException {
            ReferenceId referenceId;
            
            try {
                String response = byteString.decodeString("utf-8");
                referenceId = OBJECT_MAPPER.readValue(response, ReferenceId.class);
            } catch (NullPointerException | IOException exception) {
                referenceId = new ReferenceId(null);
            }
            
            return referenceId;
        }
    }
    
    @Override
    public PSequence<MessageProtocol> acceptResponseProtocols() {
        return TreePVector.from(Arrays.asList(
                new MessageProtocol().withContentType("text/html"),
                new MessageProtocol().withContentType("application/json")
        ));
    }
    
    @Override
    public NegotiatedSerializer<ReferenceId, ByteString> serializerForRequest() {
        return null;
    }
    
    @Override
    public NegotiatedSerializer<ReferenceId, ByteString> serializerForResponse(List<MessageProtocol> acceptedMessageProtocols) throws NotAcceptable {
        return null;
    }
}
