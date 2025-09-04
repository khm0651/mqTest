
package org.example.mqexample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotTransitionReceiver {

    @JmsListener(destination = "noti.robots.*.transitions")
    public void receiveTransitions(@Payload TransitionMessage transitionMessage, @DestinationCapture String id) {
        log.info("received transition for the robot[{}]. status: {}", id, transitionMessage.getStatus());
    }
}
