package fr.atesab.bo4hash.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest {

    @Test
    public void readWriteIntTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long seed = 64;
        Random rnd1 = new Random(seed);
        for (int i = 0; i < 100; i++) {
            IOUtils.write(out, rnd1.nextLong());
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        Random rnd2 = new Random(seed);
        for (int i = 0; i < 100; i++) {
            assertEquals(rnd2.nextLong(), IOUtils.readInt64(in));
        }
    }

}