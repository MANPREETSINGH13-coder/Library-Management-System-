package ac.bbau.library;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {
    public enum AccountStatus { PENDING, ACTIVE, REJECTED }
    public record Student(long id,String name,String fatherName,String studentId,String gender,String department,String course,String year,String semester,String address,String state,String district,String aadhaarId,String email,String mobileNumber,boolean shareDataPermission,AccountStatus status) {}
    public record Administrator(long id,String name,String email,String staffRole) {}
    public record Book(long id,String title,String author,String category,int copies,int available) {}
    public record Loan(long id,long bookId,String studentId,String dueDate,boolean returned) {}
    public record Fine(long id,String studentId,String reason,int amount,String status) {}
    private final MongoTemplate mongo;
    public LibraryService(MongoTemplate mongo){this.mongo=mongo;seed();}
    private void seed(){
        if(!mongo.exists(Query.query(Criteria.where("_id").is("library")),"counters"))insertStrings(new Document("_id","library").append("value",1000),"counters");
        if(!mongo.exists(Query.query(key("_id",1L)),"administrators"))insertStrings(new Document("_id",1L).append("name","Dr. Meera Joshi").append("email","meera.joshi@bbau.ac.in").append("staffRole","Librarian"),"administrators");
        studentSeed(201,"Manpreet Singh","STU-2024-018","manpreet@bbau.ac.in","ACTIVE"); studentSeed(202,"Milan Kumar","STU-2023-104","milan@bbau.ac.in","ACTIVE"); studentSeed(203,"Priya Sharma","STU-2025-027","priya@bbau.ac.in","PENDING");
        if(mongo.count(new Query(),"books")==0){bookSeed(101,"The Discovery of India","Jawaharlal Nehru","History",8,5);bookSeed(102,"Wings of Fire","A. P. J. Abdul Kalam","Biography",12,3);bookSeed(103,"The God of Small Things","Arundhati Roy","Fiction",6,0);bookSeed(104,"Introduction to Algorithms","Thomas H. Cormen","Computer science",5,2);bookSeed(105,"The White Tiger","Aravind Adiga","Fiction",7,1);}
        if(mongo.count(new Query(),"issued_books")==0){loanSeed(301,103,"STU-2024-018","2026-10-16");loanSeed(302,102,"STU-2023-104","2026-10-17");}
        if(mongo.count(new Query(),"fines")==0)insertStrings(new Document("_id",401L).append("studentId","STU-2024-018").append("reason","Late return").append("amount",50).append("status","DUE"),"fines");
    }
    private void studentSeed(long id,String n,String sid,String e,String s){if(!mongo.exists(Query.query(key("_id",id)),"students"))insertStrings(new Document("_id",id).append("name",n).append("studentId",sid).append("email",e).append("status",s).append("shareDataPermission",true),"students");}
    private void bookSeed(long id,String t,String a,String c,int cp,int av){insertStrings(new Document("_id",id).append("title",t).append("author",a).append("category",c).append("copies",cp).append("available",av),"books");}
    private void loanSeed(long id,long bid,String sid,String due){insertStrings(new Document("_id",id).append("bookId",bid).append("studentId",sid).append("dueDate",due).append("returned",false),"issued_books");}
    private static Criteria key(String field,long value){return Criteria.where(field).in(Long.toString(value),value);}
    private Document find(long value,String collection){return mongo.findOne(Query.query(key("_id",value)),Document.class,collection);}
    private void insertStrings(Document input,String collection){
        Document stored = new Document();
        input.forEach((field,value) -> { if(value != null) stored.put(field,String.valueOf(value)); });
        mongo.insert(stored,collection);
    }
    private Document increment(String collection,Document filter,String field,int delta){
        Document numeric = new Document("$convert",new Document("input","$"+field).append("to","long").append("onNull",0));
        Document update = new Document("$set",new Document(field,new Document("$toString",new Document("$add",List.of(numeric,delta)))));
        return mongo.getCollection(collection).findOneAndUpdate(filter,List.of(update),new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER));
    }
    private long nextId(){
        Document d=increment("counters",new Document("_id","library"),"value",1);
        if(d==null)throw new IllegalStateException("Library ID counter is unavailable.");
        return Long.parseLong(String.valueOf(d.get("value")));
    }
    private static long id(Document d){return Long.parseLong(String.valueOf(d.get("_id")));}
    private static long number(Document d,String field){return Long.parseLong(String.valueOf(d.get(field)));}
    private static String s(Document d,String field){Object value=d.get(field);return value==null?null:String.valueOf(value);}
    private static boolean flag(Document d,String field){return Boolean.parseBoolean(s(d,field));}
    private Student st(Document d){return new Student(id(d),s(d,"name"),s(d,"fatherName"),s(d,"studentId"),s(d,"gender"),s(d,"department"),s(d,"course"),s(d,"year"),s(d,"semester"),s(d,"address"),s(d,"state"),s(d,"district"),s(d,"aadhaarId"),s(d,"email"),s(d,"mobileNumber"),flag(d,"shareDataPermission"),AccountStatus.valueOf(s(d,"status")));}
    private Administrator ad(Document d){return new Administrator(id(d),s(d,"name"),s(d,"email"),s(d,"staffRole"));}
    private Book bk(Document d){return new Book(id(d),s(d,"title"),s(d,"author"),s(d,"category"),Math.toIntExact(number(d,"copies")),Math.toIntExact(number(d,"available")));}
    private Loan ln(Document d){return new Loan(id(d),number(d,"bookId"),s(d,"studentId"),s(d,"dueDate"),flag(d,"returned"));}
    private Fine fn(Document d){return new Fine(id(d),s(d,"studentId"),s(d,"reason"),Math.toIntExact(number(d,"amount")),s(d,"status"));}
    private List<Document> all(String c){return mongo.findAll(Document.class,c);}
    public Student studentProfile(String studentId) {
        Document student = mongo.findOne(Query.query(Criteria.where("studentId").regex("^" + Pattern.quote(studentId) + "$", "i")), Document.class, "students");
        if (student == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student profile not found.");
        return st(student);
    }
    public synchronized Student updateStudentProfile(String ownId, LibraryController.StudentProfileUpdate request) {
        if(request.name()==null || request.name().isBlank() || request.name().trim().length()>120) throw new IllegalArgumentException("Full name is required and must be at most 120 characters.");
        if(request.mobileNumber()!=null && !request.mobileNumber().isBlank() && !request.mobileNumber().trim().matches("[0-9]{10}")) throw new IllegalArgumentException("Mobile number must contain 10 digits.");
        if(request.aadhaarId()!=null && !request.aadhaarId().isBlank() && !request.aadhaarId().trim().matches("[0-9]{12}")) throw new IllegalArgumentException("Aadhaar ID must contain 12 digits.");
        Update update=new Update();
        String[] fields={"name","fatherName","gender","department","course","year","semester","address","state","district","aadhaarId","mobileNumber"};
        String[] values={request.name(),request.fatherName(),request.gender(),request.department(),request.course(),request.year(),request.semester(),request.address(),request.state(),request.district(),request.aadhaarId(),request.mobileNumber()};
        for(int i=0;i<fields.length;i++) {
            if(values[i]!=null) {
                if(values[i].length()>1000) throw new IllegalArgumentException("Profile field is too long.");
                update.set(fields[i],values[i].trim());
            }
        }
        if(request.shareDataPermission()!=null) update.set("shareDataPermission",request.shareDataPermission().toString());
        Document result=mongo.findAndModify(Query.query(Criteria.where("studentId").regex("^"+Pattern.quote(ownId)+"$","i")),update,FindAndModifyOptions.options().returnNew(true),Document.class,"students");
        if(result==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Student profile not found.");
        return st(result);
    }
    public synchronized Student registerStudent(String n,String fatherName,String sid,String gender,String department,String course,String year,String semester,String address,String state,String district,String aadhaarId,String e,String mobileNumber,boolean shareDataPermission){Query q=new Query(new Criteria().orOperator(Criteria.where("email").regex("^"+Pattern.quote(e)+"$","i"),Criteria.where("studentId").regex("^"+Pattern.quote(sid)+"$","i")));if(mongo.exists(q,"students"))throw new IllegalArgumentException("A student with this email or student ID already exists.");long i=nextId();insertStrings(new Document("_id",i).append("name",n).append("fatherName",fatherName).append("studentId",sid).append("gender",gender).append("department",department).append("course",course).append("year",year).append("semester",semester).append("address",address).append("state",state).append("district",district).append("aadhaarId",aadhaarId).append("email",e).append("mobileNumber",mobileNumber).append("shareDataPermission",shareDataPermission).append("status","PENDING"),"students");return st(find(i,"students"));}
    public synchronized Student reviewStudent(long i,boolean ok){Document d=mongo.findAndModify(Query.query(key("_id",i).and("status").is("PENDING")),new Update().set("status",ok?"ACTIVE":"REJECTED"),FindAndModifyOptions.options().returnNew(true),Document.class,"students");if(d==null)throw new NoSuchElementException("Student not found or already reviewed.");return st(d);}
    public synchronized Administrator createAdministrator(String n,String e,String r){if(mongo.exists(Query.query(Criteria.where("email").regex("^"+Pattern.quote(e)+"$","i")),"administrators"))throw new IllegalArgumentException("An administrator with this email already exists.");long i=nextId();insertStrings(new Document("_id",i).append("name",n).append("email",e).append("staffRole",r),"administrators");return ad(find(i,"administrators"));}
    public synchronized Book addBook(Long requestedId,String title,String author,String category,int copies,Integer requestedAvailable){
        if(title==null||title.isBlank()||author==null||author.isBlank()||category==null||category.isBlank())throw new IllegalArgumentException("Title, author and category are required.");
        if(copies<1)throw new IllegalArgumentException("At least one copy is required.");
        int available=requestedAvailable==null?copies:requestedAvailable;
        if(available<0||available>copies)throw new IllegalArgumentException("Available copies must be between zero and total copies.");
        if(requestedId!=null && (requestedId<1||requestedId>9007199254740991L))throw new IllegalArgumentException("Book ID must be a positive safe integer.");
        long id=requestedId==null?nextId():requestedId;
        if(requestedId==null){while(find(id,"books")!=null)id=nextId();}
        if(find(id,"books")!=null)throw new IllegalArgumentException("This Book ID already exists. Choose a different ID.");
        insertStrings(new Document("_id",id).append("title",title.trim()).append("author",author.trim()).append("category",category.trim()).append("copies",copies).append("available",available),"books");
        return bk(find(id,"books"));
    }
    public synchronized void deleteBook(String t){Document d=mongo.findOne(Query.query(Criteria.where("title").regex("^"+Pattern.quote(t)+"$","i")),Document.class,"books");if(d==null)throw new NoSuchElementException("Book not found.");if(mongo.exists(Query.query(key("bookId",id(d)).and("returned").in("false",false)),"issued_books"))throw new IllegalStateException("Return all issued copies before deleting this title.");mongo.remove(Query.query(key("_id",id(d))),"books");}
    public synchronized Fine addFine(String sid,String r,int a){if(a<1)throw new IllegalArgumentException("Fine amount must be at least ₹1.");long i=nextId();insertStrings(new Document("_id",i).append("studentId",sid).append("reason",r).append("amount",a).append("status","DUE"),"fines");return fn(find(i,"fines"));}
    public synchronized Fine submitFine(long i,String sid){Document d=mongo.findAndModify(Query.query(key("_id",i).and("studentId").is(sid).and("status").is("DUE")),new Update().set("status","SUBMITTED"),FindAndModifyOptions.options().returnNew(true),Document.class,"fines");if(d==null)throw new NoSuchElementException("Fine not found or already submitted.");return fn(d);}
    public synchronized Fine confirmFine(long i){Document d=mongo.findAndModify(Query.query(key("_id",i).and("status").is("SUBMITTED")),new Update().set("status","PAID"),FindAndModifyOptions.options().returnNew(true),Document.class,"fines");if(d==null)throw new NoSuchElementException("Fine not found or not submitted.");return fn(d);}
    public synchronized Loan issueBook(long bid,String sid,String due){Document filter=Query.query(key("_id",bid)).getQueryObject();filter.append("$expr",new Document("$gt",List.of(new Document("$toLong","$available"),0)));Document b=increment("books",filter,"available",-1);if(b==null)throw new IllegalStateException("Book not found or no copies are available.");long i=nextId();insertStrings(new Document("_id",i).append("bookId",bid).append("studentId",sid).append("dueDate",due).append("returned",false),"issued_books");return ln(find(i,"issued_books"));}
    public synchronized Loan returnBook(long i){Document d=mongo.findAndModify(Query.query(key("_id",i).and("returned").in("false",false)),new Update().set("returned","true"),FindAndModifyOptions.options().returnNew(true),Document.class,"issued_books");if(d==null)throw new IllegalStateException("Loan not found or already returned.");increment("books",Query.query(key("_id",number(d,"bookId"))).getQueryObject(),"available",1);return ln(d);}
    public synchronized Loan returnBook(long i,String sid){Document d=find(i,"issued_books");if(d==null||!s(d,"studentId").equalsIgnoreCase(sid))throw new NoSuchElementException("Loan not found.");return returnBook(i);}
    public List<Student> students(){return all("students").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::st).toList();} public List<Student> pendingStudents(){return students().stream().filter(x->x.status()==AccountStatus.PENDING).toList();}
    public List<Administrator> administrators(){return all("administrators").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::ad).toList();} public List<Book> books(){return all("books").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::bk).toList();}
    public List<Loan> loans(){return all("issued_books").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::ln).toList();} public List<Loan> loansForStudent(String sid){return loans().stream().filter(x->x.studentId().equalsIgnoreCase(sid)).toList();}
    public List<Fine> fines(){return all("fines").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::fn).toList();} public List<Fine> finesForStudent(String sid){return fines().stream().filter(x->x.studentId().equalsIgnoreCase(sid)).toList();}
}
