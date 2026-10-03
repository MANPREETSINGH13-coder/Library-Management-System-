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
    public record Student(long id,String name,String studentId,String email,AccountStatus status) {}
    public record Administrator(long id,String name,String email,String staffRole) {}
    public record Book(long id,String title,String author,String category,int copies,int available) {}
    public record Loan(long id,long bookId,String studentId,String dueDate,boolean returned) {}
    public record Fine(long id,String studentId,String reason,int amount,String status) {}
    private final MongoTemplate mongo;
    public LibraryService(MongoTemplate mongo){this.mongo=mongo;seed();}
    private void seed(){
        if(!mongo.exists(Query.query(Criteria.where("_id").is("library")),"counters"))mongo.insert(new Document("_id","library").append("value",1000),"counters");
        if(!mongo.exists(Query.query(Criteria.where("_id").is(1L)),"administrators"))mongo.insert(new Document("_id",1L).append("name","Dr. Meera Joshi").append("email","meera.joshi@bbau.ac.in").append("staffRole","Librarian"),"administrators");
        studentSeed(201,"Manpreet Singh","STU-2024-018","manpreet@bbau.ac.in","ACTIVE"); studentSeed(202,"Milan Kumar","STU-2023-104","milan@bbau.ac.in","ACTIVE"); studentSeed(203,"Priya Sharma","STU-2025-027","priya@bbau.ac.in","PENDING");
        if(mongo.count(new Query(),"books")==0){bookSeed(101,"The Discovery of India","Jawaharlal Nehru","History",8,5);bookSeed(102,"Wings of Fire","A. P. J. Abdul Kalam","Biography",12,3);bookSeed(103,"The God of Small Things","Arundhati Roy","Fiction",6,0);bookSeed(104,"Introduction to Algorithms","Thomas H. Cormen","Computer science",5,2);bookSeed(105,"The White Tiger","Aravind Adiga","Fiction",7,1);}
        if(mongo.count(new Query(),"issued_books")==0){loanSeed(301,103,"STU-2024-018","2026-10-16");loanSeed(302,102,"STU-2023-104","2026-10-17");}
        if(mongo.count(new Query(),"fines")==0)mongo.insert(new Document("_id",401L).append("studentId","STU-2024-018").append("reason","Late return").append("amount",50).append("status","DUE"),"fines");
    }
    private void studentSeed(long id,String n,String sid,String e,String s){if(!mongo.exists(Query.query(Criteria.where("_id").is(id)),"students"))mongo.insert(new Document("_id",id).append("name",n).append("studentId",sid).append("email",e).append("status",s),"students");}
    private void bookSeed(long id,String t,String a,String c,int cp,int av){mongo.insert(new Document("_id",id).append("title",t).append("author",a).append("category",c).append("copies",cp).append("available",av),"books");}
    private void loanSeed(long id,long bid,String sid,String due){mongo.insert(new Document("_id",id).append("bookId",bid).append("studentId",sid).append("dueDate",due).append("returned",false),"issued_books");}
    private long nextId(){return mongo.findAndModify(Query.query(Criteria.where("_id").is("library")),new Update().inc("value",1),FindAndModifyOptions.options().returnNew(true),Document.class,"counters").getLong("value");}
    private static long id(Document d){return ((Number)d.get("_id")).longValue();} private static String s(Document d,String k){return d.getString(k);}
    private Student st(Document d){return new Student(id(d),s(d,"name"),s(d,"studentId"),s(d,"email"),AccountStatus.valueOf(s(d,"status")));}
    private Administrator ad(Document d){return new Administrator(id(d),s(d,"name"),s(d,"email"),s(d,"staffRole"));}
    private Book bk(Document d){return new Book(id(d),s(d,"title"),s(d,"author"),s(d,"category"),((Number)d.get("copies")).intValue(),((Number)d.get("available")).intValue());}
    private Loan ln(Document d){return new Loan(id(d),((Number)d.get("bookId")).longValue(),s(d,"studentId"),s(d,"dueDate"),Boolean.TRUE.equals(d.getBoolean("returned")));}
    private Fine fn(Document d){return new Fine(id(d),s(d,"studentId"),s(d,"reason"),((Number)d.get("amount")).intValue(),s(d,"status"));}
    private List<Document> all(String c){return mongo.findAll(Document.class,c);}
    public synchronized Student registerStudent(String n,String sid,String e){Query q=new Query(new Criteria().orOperator(Criteria.where("email").regex("^"+Pattern.quote(e)+"$","i"),Criteria.where("studentId").regex("^"+Pattern.quote(sid)+"$","i")));if(mongo.exists(q,"students"))throw new IllegalArgumentException("A student with this email or student ID already exists.");long i=nextId();mongo.insert(new Document("_id",i).append("name",n).append("studentId",sid).append("email",e).append("status","PENDING"),"students");return st(mongo.findById(i,Document.class,"students"));}
    public synchronized Student reviewStudent(long i,boolean ok){Document d=mongo.findAndModify(Query.query(Criteria.where("_id").is(i).and("status").is("PENDING")),new Update().set("status",ok?"ACTIVE":"REJECTED"),FindAndModifyOptions.options().returnNew(true),Document.class,"students");if(d==null)throw new NoSuchElementException("Student not found or already reviewed.");return st(d);}
    public synchronized Administrator createAdministrator(String n,String e,String r){if(mongo.exists(Query.query(Criteria.where("email").regex("^"+Pattern.quote(e)+"$","i")),"administrators"))throw new IllegalArgumentException("An administrator with this email already exists.");long i=nextId();mongo.insert(new Document("_id",i).append("name",n).append("email",e).append("staffRole",r),"administrators");return ad(mongo.findById(i,Document.class,"administrators"));}
    public synchronized Book addBook(String t,String a,String c,int cp){if(cp<1)throw new IllegalArgumentException("At least one copy is required.");long i=nextId();mongo.insert(new Document("_id",i).append("title",t).append("author",a).append("category",c).append("copies",cp).append("available",cp),"books");return bk(mongo.findById(i,Document.class,"books"));}
    public synchronized void deleteBook(String t){Document d=mongo.findOne(Query.query(Criteria.where("title").regex("^"+Pattern.quote(t)+"$","i")),Document.class,"books");if(d==null)throw new NoSuchElementException("Book not found.");if(mongo.exists(Query.query(Criteria.where("bookId").is(id(d)).and("returned").is(false)),"issued_books"))throw new IllegalStateException("Return all issued copies before deleting this title.");mongo.remove(Query.query(Criteria.where("_id").is(id(d))),"books");}
    public synchronized Fine addFine(String sid,String r,int a){if(a<1)throw new IllegalArgumentException("Fine amount must be at least ₹1.");long i=nextId();mongo.insert(new Document("_id",i).append("studentId",sid).append("reason",r).append("amount",a).append("status","DUE"),"fines");return fn(mongo.findById(i,Document.class,"fines"));}
    public synchronized Fine submitFine(long i,String sid){Document d=mongo.findAndModify(Query.query(Criteria.where("_id").is(i).and("studentId").is(sid).and("status").is("DUE")),new Update().set("status","SUBMITTED"),FindAndModifyOptions.options().returnNew(true),Document.class,"fines");if(d==null)throw new NoSuchElementException("Fine not found or already submitted.");return fn(d);}
    public synchronized Fine confirmFine(long i){Document d=mongo.findAndModify(Query.query(Criteria.where("_id").is(i).and("status").is("SUBMITTED")),new Update().set("status","PAID"),FindAndModifyOptions.options().returnNew(true),Document.class,"fines");if(d==null)throw new NoSuchElementException("Fine not found or not submitted.");return fn(d);}
    public synchronized Loan issueBook(long bid,String sid,String due){Document b=mongo.findAndModify(Query.query(Criteria.where("_id").is(bid).and("available").gt(0)),new Update().inc("available",-1),FindAndModifyOptions.options().returnNew(true),Document.class,"books");if(b==null)throw new IllegalStateException("Book not found or no copies are available.");long i=nextId();mongo.insert(new Document("_id",i).append("bookId",bid).append("studentId",sid).append("dueDate",due).append("returned",false),"issued_books");return ln(mongo.findById(i,Document.class,"issued_books"));}
    public synchronized Loan returnBook(long i){Document d=mongo.findAndModify(Query.query(Criteria.where("_id").is(i).and("returned").is(false)),new Update().set("returned",true),FindAndModifyOptions.options().returnNew(true),Document.class,"issued_books");if(d==null)throw new IllegalStateException("Loan not found or already returned.");mongo.updateFirst(Query.query(Criteria.where("_id").is(((Number)d.get("bookId")).longValue())),new Update().inc("available",1),"books");return ln(d);}
    public synchronized Loan returnBook(long i,String sid){Document d=mongo.findById(i,Document.class,"issued_books");if(d==null||!s(d,"studentId").equalsIgnoreCase(sid))throw new NoSuchElementException("Loan not found.");return returnBook(i);}
    public List<Student> students(){return all("students").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::st).toList();} public List<Student> pendingStudents(){return students().stream().filter(x->x.status()==AccountStatus.PENDING).toList();}
    public List<Administrator> administrators(){return all("administrators").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::ad).toList();} public List<Book> books(){return all("books").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::bk).toList();}
    public List<Loan> loans(){return all("issued_books").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::ln).toList();} public List<Loan> loansForStudent(String sid){return loans().stream().filter(x->x.studentId().equalsIgnoreCase(sid)).toList();}
    public List<Fine> fines(){return all("fines").stream().sorted(Comparator.comparingLong(LibraryService::id)).map(this::fn).toList();} public List<Fine> finesForStudent(String sid){return fines().stream().filter(x->x.studentId().equalsIgnoreCase(sid)).toList();}
}
