package org.example.mqexample;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@ToString
@Getter
@Setter
public class TransitionMessage {
    private OffsetDateTime at;
    private String act;
    private String status;
}