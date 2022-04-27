package ru.gb.storage.commons.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
@JsonTypeInfo(// при сериализации json будет добавляться поле, кот характеризует тип данных
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        property = "type"
)

public abstract class Message implements Serializable {
}