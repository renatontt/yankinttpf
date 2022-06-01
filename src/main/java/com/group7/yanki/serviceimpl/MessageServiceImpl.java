package com.group7.yanki.serviceimpl;

import com.group7.yanki.dto.LinkRequest;
import com.group7.yanki.dto.Result;
import com.group7.yanki.dto.TransactionEvent;
import com.group7.yanki.model.Yanki;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageServiceImpl {

    @Autowired
    private StreamBridge streamBridge;

    public boolean sendToAccount(Yanki yanki){
        return streamBridge.send("toaccount-out-0",yanki);
    }

    public boolean sendResult(Result result){
        return streamBridge.send("result-out-0",result);
    }

    public boolean sendToLink(LinkRequest linkRequest){
        return streamBridge.send("link-out-0",linkRequest);
    }

    public void sendTransaction(TransactionEvent transactionEvent){
        streamBridge.send("transaction-out-0",transactionEvent);
    }

}
