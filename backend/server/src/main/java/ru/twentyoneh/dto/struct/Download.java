package ru.twentyoneh.dto.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.twentyoneh.dto.FileRecord;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class Download {
    FileRecord record;
    InputStream stream;
}
