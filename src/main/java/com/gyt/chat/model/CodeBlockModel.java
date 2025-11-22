package com.gyt.chat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CodeBlockModel {
    private final String code;
    private final boolean isCodeBlock;
}
