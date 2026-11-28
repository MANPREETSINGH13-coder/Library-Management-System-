package ac.bbau.library;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StudentProfileTest {
    private LibraryController.StudentProfileUpdate request(String mobile, String aadhaar) {
        return new LibraryController.StudentProfileUpdate(" New Name ", " Father ", "Male", "Computer Science", "BCA", "2nd Year", "3", " Address ", "Uttar Pradesh", "Lucknow", aadhaar, mobile, true);
    }
    @Test void updateUsesSignedInIdentity() {
        AuthService auth=mock(AuthService.class);
        LibraryService library=mock(LibraryService.class);
        when(auth.requireStudentId("Bearer token")).thenReturn("OWN-201");
        var body=request("9876543210","123456789012");
        var result=new LibraryService.Student(201,"New Name","Father","OWN-201","Male","Computer Science","BCA","2nd Year","3","Address","Uttar Pradesh","Lucknow","123456789012","test@example.invalid","9876543210",true,LibraryService.AccountStatus.ACTIVE);
        when(library.updateStudentProfile("OWN-201",body)).thenReturn(result);
        assertSame(result,new LibraryController(library,auth).updateStudentProfile("Bearer token",body));
        verify(auth).updateProfile("Bearer token","New Name",null,null);
        verify(library).updateStudentProfile("OWN-201",body);
    }
    @Test void nonStudentCannotUpdateStudentDetails() {
        AuthService auth=mock(AuthService.class);
        LibraryService library=mock(LibraryService.class);
        when(auth.requireStudentId(anyString())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class,()->new LibraryController(library,auth).updateStudentProfile("Bearer staff",request("","")));
        verifyNoInteractions(library);
    }
    @Test void invalidMobileDoesNotWrite() {
        MongoTemplate mongo=mock(MongoTemplate.class);
        LibraryService library=new LibraryService(mongo);
        clearInvocations(mongo);
        assertThrows(IllegalArgumentException.class,()->library.updateStudentProfile("OWN-201",request("123","")));
        verifyNoInteractions(mongo);
    }
    @Test void invalidAadhaarDoesNotWrite() {
        MongoTemplate mongo=mock(MongoTemplate.class);
        LibraryService library=new LibraryService(mongo);
        clearInvocations(mongo);
        assertThrows(IllegalArgumentException.class,()->library.updateStudentProfile("OWN-201",request("","123")));
        verifyNoInteractions(mongo);
    }
    @Test void updateWhitelistsFieldsAndKeepsStringStorage() {
        MongoTemplate mongo=mock(MongoTemplate.class);
        LibraryService library=new LibraryService(mongo);
        clearInvocations(mongo);
        Document saved=new Document("_id","201").append("studentId","OWN-201").append("name","New Name").append("status","ACTIVE");
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(Document.class),eq("students"))).thenReturn(saved);
        library.updateStudentProfile("OWN-201",request("",""));
        var query=ArgumentCaptor.forClass(Query.class);
        var update=ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(),update.capture(),any(FindAndModifyOptions.class),eq(Document.class),eq("students"));
        Document set=(Document)update.getValue().getUpdateObject().get("$set");
        assertEquals("New Name",set.get("name"));
        assertEquals("true",set.get("shareDataPermission"));
        for(String field:new String[]{"_id","studentId","email","status","role","passwordHash"}) assertFalse(set.containsKey(field));
        assertTrue(query.getValue().getQueryObject().toJson().contains("OWN-201"));
    }
}
