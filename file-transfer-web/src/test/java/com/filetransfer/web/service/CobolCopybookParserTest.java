package com.filetransfer.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CobolCopybookParserTest {

    private CobolCopybookParser parser;

    @BeforeEach
    void setUp() {
        parser = new CobolCopybookParser();
    }

    @Test
    void testParseCopybookBasic() {
        String copybook = """
            01 CUSTOMER-RECORD.
               05 CUSTOMER-ID      PIC 9(6).
               05 CUSTOMER-NAME    PIC X(30).
               05 CUSTOMER-BALANCE PIC 9(8)V99.
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);

        assertNotNull(schema);
        assertEquals(3, schema.getFields().size());
        assertEquals(48, schema.getTotalLength()); // 6 + 30 + 10 + 2 for decimal

        CobolCopybookParser.CobolField firstField = schema.getFields().get(0);
        assertEquals("CUSTOMER-ID", firstField.getName());
        assertEquals(6, firstField.getLength());
        assertEquals(CobolCopybookParser.DataType.NUMERIC, firstField.getPicture().getDataType());
    }

    @Test
    void testParseCopybookWithOccurs() {
        String copybook = """
            01 ORDER-RECORD.
               05 ORDER-ID         PIC 9(8).
               05 LINE-ITEMS       OCCURS 10 TIMES.
                  10 ITEM-CODE     PIC X(5).
                  10 QUANTITY      PIC 9(3).
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);

        assertNotNull(schema);
        // Should have ORDER-ID + 10 occurrences of LINE-ITEMS
        assertTrue(schema.getFields().size() >= 2);
        assertEquals(88, schema.getTotalLength()); // 8 + (5+3)*10
    }

    @Test
    void testValidateDataValid() {
        String copybook = """
            01 SIMPLE-RECORD.
               05 ID-FIELD    PIC 9(5).
               05 NAME-FIELD  PIC X(10).
            """;

        String validData = "12345JOHN      \n67890JANE      ";

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.ValidationResult result = parser.validateData(schema, validData);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
        assertEquals(2, result.getValidLines());
    }

    @Test
    void testValidateDataInvalid() {
        String copybook = """
            01 SIMPLE-RECORD.
               05 ID-FIELD    PIC 9(5).
               05 NAME-FIELD  PIC X(10).
            """;

        String invalidData = "ABCDEFGHIJKLMNO"; // Letters in numeric field

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.ValidationResult result = parser.validateData(schema, invalidData);

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("Invalid numeric value"));
    }

    @Test
    void testValidateDataWrongLength() {
        String copybook = """
            01 RECORD.
               05 FIELD1 PIC X(10).
            """;

        String shortData = "SHORT"; // Only 5 characters, expected 10

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.ValidationResult result = parser.validateData(schema, shortData);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Expected length"));
    }

    @Test
    void testParsePictureClauseNumeric() {
        String copybook = """
            01 NUMERIC-RECORD.
               05 AMOUNT PIC 9(8)V99.
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.CobolField field = schema.getFields().get(0);

        assertEquals(CobolCopybookParser.DataType.NUMERIC, field.getPicture().getDataType());
        assertEquals(10, field.getLength()); // 8 integer + 2 decimal
    }

    @Test
    void testParsePictureClauseSigned() {
        String copybook = """
            01 SIGNED-RECORD.
               05 BALANCE PIC S9(7)V99.
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.CobolField field = schema.getFields().get(0);

        assertEquals(CobolCopybookParser.DataType.SIGNED_NUMERIC, field.getPicture().getDataType());
        assertEquals(9, field.getLength()); // 7 integer + 2 decimal
    }

    @Test
    void testParsePictureClauseAlphabetic() {
        String copybook = """
            01 TEXT-RECORD.
               05 NAME PIC A(20).
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.CobolField field = schema.getFields().get(0);

        assertEquals(CobolCopybookParser.DataType.ALPHABETIC, field.getPicture().getDataType());
        assertEquals(20, field.getLength());
    }

    @Test
    void testValidateAlphabeticField() {
        String copybook = """
            01 ALPHA-RECORD.
               05 LETTERS PIC A(5).
            """;

        String validData = "HELLO";
        String invalidData = "HEL12"; // Contains numbers

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        
        CobolCopybookParser.ValidationResult validResult = parser.validateData(schema, validData);
        assertTrue(validResult.isValid());

        CobolCopybookParser.ValidationResult invalidResult = parser.validateData(schema, invalidData);
        assertFalse(invalidResult.isValid());
        assertTrue(invalidResult.getErrors().get(0).contains("Invalid alphabetic value"));
    }

    @Test
    void testEmptyFile() {
        String copybook = """
            01 RECORD.
               05 FIELD PIC X(10).
            """;

        String emptyData = "";

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);
        CobolCopybookParser.ValidationResult result = parser.validateData(schema, emptyData);

        assertTrue(result.isValid()); // Empty file is considered valid
        assertEquals(0, result.getValidLines());
    }

    @Test
    void testInvalidCopybook() {
        String invalidCopybook = "This is not a valid COBOL copybook";

        assertThrows(RuntimeException.class, () -> {
            parser.parseCopybook(invalidCopybook);
        });
    }

    @Test
    void testComplexCopybook() {
        String copybook = """
            01 EMPLOYEE-RECORD.
               05 EMP-ID           PIC 9(6).
               05 EMP-NAME         PIC X(25).
               05 EMP-DEPT         PIC X(10).
               05 EMP-SALARY       PIC 9(8)V99.
               05 EMP-START-DATE.
                  10 START-YEAR    PIC 9(4).
                  10 START-MONTH   PIC 9(2).
                  10 START-DAY     PIC 9(2).
               05 DEPENDENTS       OCCURS 5 TIMES.
                  10 DEP-NAME      PIC X(20).
                  10 DEP-AGE       PIC 9(2).
            """;

        CobolCopybookParser.CobolSchema schema = parser.parseCopybook(copybook);

        assertNotNull(schema);
        assertTrue(schema.getFields().size() >= 5);
        
        // Calculate expected length: 6+25+10+10+8+5*(20+2) = 169
        assertEquals(169, schema.getTotalLength());
    }

    @Test
    void testValidationResultMerge() {
        CobolCopybookParser.ValidationResult result1 = new CobolCopybookParser.ValidationResult();
        result1.addError("Error 1");
        result1.addWarning("Warning 1");
        result1.incrementValidLines();

        CobolCopybookParser.ValidationResult result2 = new CobolCopybookParser.ValidationResult();
        result2.addError("Error 2");
        result2.addWarning("Warning 2");
        result2.incrementValidLines();
        result2.incrementValidLines();

        result1.merge(result2);

        assertEquals(2, result1.getErrors().size());
        assertEquals(2, result1.getWarnings().size());
        assertEquals(3, result1.getValidLines());
        assertFalse(result1.isValid()); // Should be false due to errors
    }
}