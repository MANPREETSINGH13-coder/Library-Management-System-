package ac.bbau.library.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

/** JavaFX desktop frontend for the BBAU Library REST API. */
public final class LibraryDesktopApplication extends Application {
    private static final ObjectMapper DATA_JSON = new ObjectMapper();
    private static final Map<String, List<String>> ACADEMIC_OPTIONS = loadOptions("academic-options.json");
    private static final Map<String, List<String>> INDIA_LOCATIONS = loadOptions("india-locations.json");
    private static final List<String> COMMON_COURSES = List.of("B.Tech", "BCA", "BBA", "B.Com", "B.Sc", "BA", "B.Pharm", "B.Ed", "LLB", "BBA LL.B", "M.Tech", "MCA", "MBA", "M.Com", "M.Sc", "MA", "M.Pharm", "M.Ed", "LLM", "Ph.D", "Diploma");
    private final ApiClient api = new ApiClient();
    private Stage stage;
    private Session session;
    private BorderPane shell;
    private VBox pageHost;
    private Label notice;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("BBAU University Library");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxWidth(460);
        panel.getStyleClass().add("auth-card");
        Label eyebrow = new Label("BBAU UNIVERSITY LIBRARY"); eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Welcome back"); title.getStyleClass().add("auth-title");
        Label intro = new Label("Sign in to your library account."); intro.getStyleClass().add("muted");
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("Student", "Administration", "Super Admin"));
        role.setValue("Student"); role.setMaxWidth(Double.MAX_VALUE);
        TextField email = new TextField(); email.setPromptText("Email address");
        PasswordField password = new PasswordField(); password.setPromptText("Password");
        Label error = new Label(); error.getStyleClass().add("error-text"); error.setWrapText(true);
        Button signIn = primaryButton("Sign in"); signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setOnAction(event -> {
            error.setText(""); signIn.setDisable(true);
            api.post("/api/auth/login", ApiClient.data("email", email.getText().trim(), "password", password.getText(), "role", role.getValue()), null)
                    .whenComplete((data, failure) -> Platform.runLater(() -> {
                        signIn.setDisable(false);
                        if (failure != null) { error.setText(message(failure)); return; }
                        session = new Session(data.path("token").asText(), data.path("role").asText(), data.path("name").asText(), data.path("studentId").asText());
                        showDashboard();
                    }));
        });
        Hyperlink register = new Hyperlink("New student? Create an account"); register.setOnAction(event -> showRegistration());
        Label endpoint = new Label("API: " + api.baseUrl()); endpoint.getStyleClass().add("small-muted");
        panel.getChildren().addAll(eyebrow, title, intro, labeled("Account type", role), labeled("Email", email), labeled("Password", password), error, signIn, register, endpoint);
        VBox brand = new VBox(15);
        brand.getStyleClass().add("brand-panel");
        Label brandName = new Label("BBAU\nUNIVERSITY LIBRARY"); brandName.getStyleClass().add("brand-title");
        Label message = new Label("Knowledge for everyone.\nBooks for every journey."); message.getStyleClass().add("brand-quote");
        Label presenter = new Label("Library Management System • Lucknow"); presenter.getStyleClass().add("brand-caption");
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        brand.getChildren().addAll(brandName, spacer, message, presenter);
        HBox body = new HBox(0, brand, centered(panel));
        HBox.setHgrow(brand, Priority.ALWAYS); HBox.setHgrow(body.getChildren().get(1), Priority.ALWAYS);
        Scene scene = new Scene(body, 1180, 760); scene.getStylesheets().add(css());
        stage.setScene(scene);
    }

    private void showRegistration() {
        TextField name = new TextField(), father = new TextField(), id = new TextField(), email = new TextField();
        TextField mobile = new TextField(), aadhaar = new TextField(), address = new TextField();
        PasswordField password = new PasswordField(), confirm = new PasswordField();
        ComboBox<String> gender = options("Select gender", "Male", "Female", "Other", "Non-binary", "Prefer not to say");
        ComboBox<String> department = options("Select department", ACADEMIC_OPTIONS.keySet().stream().sorted().toArray(String[]::new));
        ComboBox<String> course = options("Select course"); course.setDisable(true);
        department.valueProperty().addListener((obs, old, value) -> {
            List<String> choices = new ArrayList<>(ACADEMIC_OPTIONS.getOrDefault(value, List.of()));
            choices.addAll(COMMON_COURSES); choices.add("Other / not listed");
            course.getItems().setAll(choices.stream().distinct().toList()); course.setValue(null); course.setDisable(value == null);
        });
        ComboBox<String> year = options("Select year", "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "Research Scholar");
        ComboBox<String> semester = options("Select semester", "Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8", "Semester 9", "Semester 10", "Not applicable");
        ComboBox<String> state = options("Select state / UT", INDIA_LOCATIONS.keySet().stream().sorted().toArray(String[]::new));
        ComboBox<String> districtChoice = options("Select district"); districtChoice.setDisable(true);
        state.valueProperty().addListener((obs, old, value) -> {
            List<String> choices = new ArrayList<>(INDIA_LOCATIONS.getOrDefault(value, List.of()));
            choices.add("Other / not listed");
            districtChoice.getItems().setAll(choices); districtChoice.setValue(null); districtChoice.setEditable(value != null); districtChoice.setDisable(value == null);
            districtChoice.setPromptText(value == null ? "Select state first" : "Select or type district");
        });
        name.setPromptText("Enter full name"); father.setPromptText("Enter father's name"); id.setPromptText("University enrollment number");
        email.setPromptText("name@bbau.ac.in"); mobile.setPromptText("10-digit mobile number"); aadhaar.setPromptText("12-digit Aadhaar ID");
        address.setPromptText("House / street / locality");
        mobile.addEventFilter(KeyEvent.KEY_TYPED, e -> { if (!e.getCharacter().matches("[0-9]") || mobile.getText().length() >= 10) e.consume(); });
        aadhaar.addEventFilter(KeyEvent.KEY_TYPED, e -> { if (!e.getCharacter().matches("[0-9]") || aadhaar.getText().length() >= 12) e.consume(); });
        CheckBox consent = new CheckBox("I consent to BBAU Library using these details to create and manage my student membership.");
        consent.setWrapText(true); consent.getStyleClass().add("registration-consent");
        Label error = new Label(); error.getStyleClass().add("error-text"); error.setWrapText(true);
        Button submit = primaryButton("Submit registration"); submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> {
            if (name.getText().isBlank() || father.getText().isBlank() || id.getText().isBlank() || email.getText().isBlank()
                    || mobile.getText().length() != 10 || aadhaar.getText().length() != 12 || gender.getValue() == null || department.getValue() == null
                    || course.getValue() == null || year.getValue() == null || semester.getValue() == null
                    || address.getText().isBlank() || state.getValue() == null || districtValue(districtChoice).isBlank()) {
                error.setText("Please complete all required fields. Mobile number must contain 10 digits."); return;
            }
            if (!email.getText().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) { error.setText("Enter a valid email address."); return; }
            if (!password.getText().equals(confirm.getText())) { error.setText("Passwords do not match."); return; }
            if (password.getText().length() < 8) { error.setText("Password must contain at least 8 characters."); return; }
            if (!consent.isSelected()) { error.setText("Please provide permission to process your registration details."); return; }
            submit.setDisable(true); error.setText("");
            api.post("/api/students/register", ApiClient.data("name", name.getText().trim(), "fatherName", father.getText().trim(),
                    "studentId", id.getText().trim(), "gender", gender.getValue(), "department", department.getValue(),
                    "course", course.getValue(), "year", year.getValue(), "semester", semester.getValue(), "address", address.getText().trim(),
                    "state", state.getValue(), "district", districtValue(districtChoice), "aadhaarId", aadhaar.getText().trim(),
                    "password", password.getText(), "confirmPassword", confirm.getText(), "email", email.getText().trim(),
                    "mobileNumber", mobile.getText().trim(), "shareDataPermission", consent.isSelected()), null)
                    .whenComplete((data, failure) -> Platform.runLater(() -> {
                        submit.setDisable(false);
                        if (failure != null) { error.setText(message(failure)); return; }
                        Alert confirmation = new Alert(Alert.AlertType.INFORMATION, "Registration submitted. Administration approval is required before sign-in.", ButtonType.OK);
                        confirmation.setHeaderText("Registration received");
                        confirmation.showAndWait();
                        showLogin();
                    }));
        });
        Button back = new Button("← Back to sign in"); back.getStyleClass().add("registration-back"); back.setOnAction(event -> showLogin());
        VBox form = new VBox(12);
        form.getStyleClass().add("registration-form");
        Label eyebrow = new Label("BBAU UNIVERSITY LIBRARY"); eyebrow.getStyleClass().add("eyebrow");
        Label title = heading("Student registration");
        Label intro = new Label("Complete your university details to request library membership."); intro.getStyleClass().add("muted");
        form.getChildren().addAll(back, eyebrow, title, intro,
                sectionTitle("Personal details"), row(field("Full name *", name), field("Father's name *", father)),
                row(field("Enrollment number *", id), field("Gender *", gender)),
                sectionTitle("Academic details"), row(field("Department *", department), field("Course *", course)),
                row(field("Current year *", year), field("Current semester *", semester)),
                sectionTitle("Contact and address"), row(field("University / personal email *", email), field("Mobile number *", mobile)),
                field("Address *", address), row(field("State / UT *", state), field("District *", districtChoice)),
                field("Aadhaar ID *", aadhaar),
                sectionTitle("Account security"), row(field("Password (8 characters minimum) *", password), field("Confirm password *", confirm)),
                consent, error, submit,
                new Label("Your account remains pending until Administration approves it.") {{ getStyleClass().add("registration-note"); }});
        VBox brand = new VBox(15); brand.getStyleClass().addAll("brand-panel", "registration-brand");
        Label brandName = new Label("BBAU\nUNIVERSITY LIBRARY"); brandName.getStyleClass().add("brand-title");
        Label quote = new Label("Knowledge for everyone.\nBooks for every journey."); quote.getStyleClass().add("brand-quote");
        Label brandCaption = new Label("BABASAHEB BHIMRAO AMBEDKAR UNIVERSITY · LUCKNOW"); brandCaption.getStyleClass().add("brand-caption");
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); brand.getChildren().addAll(brandName, spacer, quote, brandCaption);
        ScrollPane scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.getStyleClass().add("registration-scroll");
        StackPane formSide = new StackPane(scroll); formSide.getStyleClass().add("registration-side");
        HBox body = new HBox(0, brand, formSide); HBox.setHgrow(formSide, Priority.ALWAYS);
        Scene scene = new Scene(body, 1180, 850); scene.getStylesheets().add(css()); stage.setScene(scene);
    }

    private static Map<String, List<String>> loadOptions(String fileName) {
        String resource = "/ac/bbau/library/desktop/" + fileName;
        try (InputStream input = LibraryDesktopApplication.class.getResourceAsStream(resource)) {
            if (input == null) return Map.of();
            return DATA_JSON.readValue(input, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception exception) { throw new IllegalStateException("Could not load registration options: " + fileName, exception); }
    }

    private ComboBox<String> options(String prompt, String... values) {
        ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(values)); box.setPromptText(prompt); box.setMaxWidth(Double.MAX_VALUE); return box;
    }
    private VBox field(String label, Control control) { return labeled(label, control); }
    private HBox row(Node left, Node right) { HBox row = new HBox(12, left, right); HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS); return row; }
    private String districtValue(ComboBox<String> district) { return district.isEditable() ? district.getEditor().getText().trim() : blank(district.getValue(), ""); }
    private Label sectionTitle(String title) { Label label = new Label(title); label.getStyleClass().add("registration-section"); return label; }

    private void showDashboard() {
        shell = new BorderPane(); shell.getStyleClass().add("app-shell");
        shell.setLeft(sidebar());
        shell.setTop(topbar());
        pageHost = new VBox(18); pageHost.getStyleClass().add("page-host");
        ScrollPane scroll = new ScrollPane(pageHost); scroll.setFitToWidth(true); scroll.getStyleClass().add("page-scroll");
        shell.setCenter(scroll);
        Scene scene = new Scene(shell, 1360, 850); scene.getStylesheets().add(css()); stage.setScene(scene);
        showPage("Overview");
    }

    private VBox sidebar() {
        VBox side = new VBox(12); side.getStyleClass().add("sidebar"); side.setPrefWidth(245);
        Label logo = new Label("BB"); logo.getStyleClass().add("logo-mark");
        Label title = new Label("BBAU\nUNIVERSITY LIBRARY"); title.getStyleClass().add("sidebar-title");
        HBox brand = new HBox(12, logo, title); brand.setAlignment(Pos.CENTER_LEFT); brand.setPadding(new Insets(4, 0, 28, 0));
        Label role = new Label(session.role().toUpperCase() + " PORTAL"); role.getStyleClass().add("sidebar-caption");
        side.getChildren().addAll(brand, role);
        for (String page : pagesForRole()) {
            Button nav = new Button(page); nav.getStyleClass().add("nav-button"); nav.setMaxWidth(Double.MAX_VALUE);
            nav.setOnAction(event -> showPage(page)); side.getChildren().add(nav);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Label apiLabel = new Label("Connected API\n" + api.baseUrl()); apiLabel.getStyleClass().add("sidebar-caption"); apiLabel.setWrapText(true);
        side.getChildren().addAll(spacer, apiLabel);
        return side;
    }

    private List<String> pagesForRole() {
        return switch (session.role()) {
            case "Student" -> List.of("Overview", "Browse books", "My issued books", "OCR scanner", "My fines", "My profile");
            case "Administration" -> List.of("Overview", "Students", "Student approvals", "Books", "Issue / return", "OCR scanner", "Fines", "Reports", "My profile");
            default -> List.of("Overview", "Registered students", "Administrators", "System settings", "My profile");
        };
    }

    private HBox topbar() {
        Label who = new Label(session.name() + "   •   " + session.role()); who.getStyleClass().add("topbar-user");
        Button logout = new Button("Sign out"); logout.getStyleClass().add("outline-button");
        logout.setOnAction(event -> api.post("/api/auth/logout", Map.of(), session.token()).whenComplete((data, failure) -> Platform.runLater(() -> { session = null; showLogin(); })));
        HBox bar = new HBox(14, who, logout); bar.setAlignment(Pos.CENTER_RIGHT); bar.getStyleClass().add("topbar");
        HBox.setHgrow(who, Priority.ALWAYS); return bar;
    }

    private void showPage(String page) {
        pageHost.getChildren().clear();
        notice = new Label(); notice.getStyleClass().add("notice"); notice.setWrapText(true); notice.setVisible(false); notice.setManaged(false);
        pageHost.getChildren().addAll(new Label("BBAU LIBRARY  /  " + page.toUpperCase()), heading(page), notice);
        switch (page) {
            case "Overview" -> overview();
            case "Browse books" -> booksPage(false);
            case "Books" -> booksPage(true);
            case "Students" -> studentsPage(false);
            case "Student approvals" -> studentsPage(true);
            case "Issue / return" -> loansPage(false);
            case "My issued books" -> loansPage(true);
            case "OCR scanner" -> scannerPage();
            case "Fines", "My fines" -> finesPage();
            case "Administrators" -> administratorsPage();
            case "Registered students" -> registeredStudentsPage();
            case "Reports" -> reportsPage();
            case "System settings" -> systemSettingsPage();
            case "My profile" -> profilePage();
            default -> pageHost.getChildren().add(new Label("This section is not available for this role."));
        }
    }

    private void overview() {
        Label intro = new Label("Manage books, student access, and circulation from your library workspace."); intro.getStyleClass().add("muted");
        pageHost.getChildren().add(intro);
        VBox apiCard = statCard("Library API", "Checking...");
        VBox bookCard = statCard("Book catalogue", "Loading...");
        HBox cards = new HBox(14); cards.getChildren().addAll(statCard("Your role", session.role()), apiCard, bookCard);
        pageHost.getChildren().add(cards);
        run(api.get("/api/health", null), data -> ((Label) apiCard.getChildren().get(1)).setText("Online"));
        run(api.get("/api/books", null), data -> {
            List<Book> books = api.list(data, new TypeReference<List<Book>>() {});
            ((Label) bookCard.getChildren().get(1)).setText(books.size() + " titles");
        });
        String destination = session.role().equals("Student") ? "Browse books" : session.role().equals("Administration") ? "Students" : "Registered students";
        Button open = primaryButton("Open " + destination.toLowerCase()); open.setOnAction(event -> showPage(destination)); pageHost.getChildren().add(open);
        if (session.role().equals("Administration")) {
            VBox memberCard = statCard("Student accounts", "Loading...");
            VBox loanCard = statCard("Issued books", "Loading...");
            cards.getChildren().addAll(memberCard, loanCard);
            run(api.get("/api/admin/students?pendingOnly=false", token()), data -> {
                List<Student> students = api.list(data, new TypeReference<List<Student>>() {});
                ((Label) memberCard.getChildren().get(1)).setText(Integer.toString(students.size()));
            });
            run(api.get("/api/admin/loans", token()), data -> {
                List<Loan> loans = api.list(data, new TypeReference<List<Loan>>() {});
                ((Label) loanCard.getChildren().get(1)).setText(Long.toString(loans.stream().filter(loan -> !loan.returned()).count()));
            });
        } else if (session.role().equals("Super Admin")) {
            VBox adminCard = statCard("Administration accounts", "Loading...");
            VBox studentCard = statCard("Student accounts", "Loading...");
            cards.getChildren().addAll(adminCard, studentCard);
            run(api.get("/api/super-admin/administrators", token()), data -> {
                List<Administrator> administrators = api.list(data, new TypeReference<List<Administrator>>() {});
                ((Label) adminCard.getChildren().get(1)).setText(Integer.toString(administrators.size()));
            });
            run(api.get("/api/super-admin/students", token()), data -> {
                List<StudentSummary> students = api.list(data, new TypeReference<List<StudentSummary>>() {});
                ((Label) studentCard.getChildren().get(1)).setText(Integer.toString(students.size()));
            });
        } else {
            VBox issuedCard = statCard("Your issued books", "Loading...");
            VBox fineCard = statCard("Outstanding fines", "Loading...");
            cards.getChildren().addAll(issuedCard, fineCard);
            run(api.get("/api/student/loans?studentId=" + api.encode(session.studentId()), token()), data -> {
                List<Loan> loans = api.list(data, new TypeReference<List<Loan>>() {});
                ((Label) issuedCard.getChildren().get(1)).setText(Long.toString(loans.stream().filter(loan -> !loan.returned()).count()));
            });
            run(api.get("/api/student/fines?studentId=" + api.encode(session.studentId()), token()), data -> {
                List<Fine> fines = api.list(data, new TypeReference<List<Fine>>() {});
                ((Label) fineCard.getChildren().get(1)).setText(Long.toString(fines.stream().filter(fine -> !"PAID".equalsIgnoreCase(fine.status())).count()));
            });
        }
    }

    private void booksPage(boolean admin) {
        TableView<Book> table = table();
        col(table,"ID",x->Long.toString(x.id())); col(table,"Title",Book::title); col(table,"Author",Book::author);
        col(table,"Category",Book::category); col(table,"Copies",x->x.copies()+" / "+x.available()+" available");
        Button refresh = new Button("Refresh"); refresh.setOnAction(e->loadBooks(table));
        HBox actions = new HBox(10, refresh);
        if (admin) {
            Button add=primaryButton("Add book"); add.setOnAction(e->addBook(table));
            Button delete=new Button("Delete selected"); delete.setOnAction(e->{Book selected=table.getSelectionModel().getSelectedItem();if(selected==null){notice("Select a book first.");return;}deleteBook(selected,table);});
            actions.getChildren().addAll(add,delete);
        } else {
            Button issue=primaryButton("Request issue"); issue.setOnAction(e->{Book selected=table.getSelectionModel().getSelectedItem();if(selected==null){notice("Select a book first.");return;}if(selected.available()<1){notice("No copies are currently available.");return;}issueBook(selected.id(),()->loadBooks(table));}); actions.getChildren().add(issue);
        }
        pageHost.getChildren().addAll(actions,table); VBox.setVgrow(table,Priority.ALWAYS); loadBooks(table);
    }

    private void loadBooks(TableView<Book> table) { run(api.get("/api/books",token()), data->{table.setItems(FXCollections.observableArrayList(api.list(data,new TypeReference<List<Book>>(){})));}); }

    private void studentsPage(boolean pendingOnly) {
        TableView<Student> table=table(); col(table,"Name",Student::name);col(table,"Student ID",Student::studentId);col(table,"Email",Student::email);col(table,"Status",Student::status);
        Button refresh=new Button("Refresh");refresh.setOnAction(e->loadStudents(table,pendingOnly));
        HBox actions=new HBox(10,refresh);
        if (pendingOnly) {
            Button approve=primaryButton("Approve selected");approve.setOnAction(e->review(table,true,pendingOnly));
            Button reject=new Button("Reject selected");reject.getStyleClass().add("outline-button");reject.setOnAction(e->review(table,false,pendingOnly));
            actions.getChildren().addAll(approve,reject);
        }
        pageHost.getChildren().addAll(new Label(pendingOnly ? "Review new student registration requests." : "Browse all student accounts and their membership status."),actions,table);
        VBox.setVgrow(table,Priority.ALWAYS);loadStudents(table,pendingOnly);
    }

    private void loadStudents(TableView<Student> table, boolean pendingOnly) { run(api.get("/api/admin/students?pendingOnly="+pendingOnly,token()),data->table.setItems(FXCollections.observableArrayList(api.list(data,new TypeReference<List<Student>>(){})))); }
    private void review(TableView<Student> table,boolean approved,boolean pendingOnly) { Student s=table.getSelectionModel().getSelectedItem();if(s==null){notice("Select a student first.");return;}run(api.post("/api/admin/students/"+s.id()+"/review",ApiClient.data("approve",approved),token()),d->{notice(approved?"Student approved.":"Student rejected.");loadStudents(table,pendingOnly);}); }

    private void loansPage(boolean studentOnly) {
        TableView<Loan> table=table();col(table,"Issue ID",x->Long.toString(x.id()));col(table,"Book ID",x->Long.toString(x.bookId()));col(table,"Student ID",Loan::studentId);col(table,"Due date",Loan::dueDate);col(table,"Status",x->x.returned()?"Returned":"Issued");
        Button refresh=new Button("Refresh");refresh.setOnAction(e->loadLoans(table,studentOnly));
        HBox actions=new HBox(10,refresh);
        if(!studentOnly){Button issue=primaryButton("Issue book");issue.setOnAction(e->issueBook(null,()->loadLoans(table,false)));actions.getChildren().add(issue);}
        Button giveBack=primaryButton("Return selected book");giveBack.setOnAction(e->{Loan l=table.getSelectionModel().getSelectedItem();if(l==null){notice("Select an issued book first.");return;}if(l.returned()){notice("This book is already returned.");return;}returnBook(l,table,studentOnly);});actions.getChildren().add(giveBack);
        pageHost.getChildren().addAll(actions,table);VBox.setVgrow(table,Priority.ALWAYS);loadLoans(table,studentOnly);
    }

    private void loadLoans(TableView<Loan> table,boolean studentOnly) {
        String path=studentOnly?"/api/student/loans?studentId="+api.encode(session.studentId()):"/api/admin/loans";
        run(api.get(path,token()),data->table.setItems(FXCollections.observableArrayList(api.list(data,new TypeReference<List<Loan>>(){}))));
    }

    private void issueBook(Long presetBookId,Runnable refresh) {
        TextField bookId=new TextField(presetBookId==null?"":presetBookId.toString());bookId.setPromptText("Book ID");
        TextField studentId=new TextField();studentId.setPromptText("Student ID");
        DatePicker due=new DatePicker(LocalDate.now().plusDays(14));
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Issue a book");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        VBox fields=new VBox(12,labeled("Book ID",bookId));if(session.role().equals("Administration"))fields.getChildren().add(labeled("Student ID",studentId));fields.getChildren().add(labeled("Due date",due));dialog.getDialogPane().setContent(fields);
        dialog.showAndWait().filter(x->x==ButtonType.OK).ifPresent(x->{try{long bid=Long.parseLong(bookId.getText().trim());String path=session.role().equals("Student")?"/api/student/loans":"/api/admin/loans";Map<String,Object> body=session.role().equals("Student")?ApiClient.data("bookId",bid,"dueDate",due.getValue().toString()):ApiClient.data("bookId",bid,"studentId",studentId.getText().trim(),"dueDate",due.getValue().toString());run(api.post(path,body,token()),d->{notice("Book issued successfully.");refresh.run();});}catch(NumberFormatException ex){notice("Enter a valid numeric Book ID.");}});
    }

    private void returnBook(Loan loan,TableView<Loan> table,boolean studentOnly) {
        String path=studentOnly?"/api/student/loans/"+loan.id()+"/return":"/api/admin/loans/"+loan.id()+"/return";
        run(api.post(path,Map.of(),token()),d->{notice("Book return recorded.");loadLoans(table,studentOnly);});
    }

    private void finesPage() {
        TableView<Fine> table=table();col(table,"Fine ID",x->Long.toString(x.id()));col(table,"Student ID",Fine::studentId);col(table,"Reason",Fine::reason);col(table,"Amount",x->"₹"+x.amount());col(table,"Status",Fine::status);
        boolean admin=session.role().equals("Administration");
        Button refresh=new Button("Refresh");refresh.setOnAction(e->loadFines(table));HBox actions=new HBox(10,refresh);
        if(admin){Button add=primaryButton("Record fine");add.setOnAction(e->addFine(table));Button confirm=primaryButton("Confirm submitted");confirm.setOnAction(e->{Fine f=table.getSelectionModel().getSelectedItem();if(f==null){notice("Select a fine first.");return;}run(api.post("/api/admin/fines/"+f.id()+"/confirm",Map.of(),token()),d->{notice("Fine payment confirmed.");loadFines(table);});});actions.getChildren().addAll(add,confirm);}
        else {Button submit=primaryButton("Submit payment");submit.setOnAction(e->{Fine f=table.getSelectionModel().getSelectedItem();if(f==null){notice("Select a due fine first.");return;}if(!f.status().equals("DUE")){notice("Only due fines can be submitted.");return;}run(api.post("/api/student/fines/"+f.id()+"/submit",ApiClient.data("studentId",session.studentId()),token()),d->{notice("Fine submitted for Administration review.");loadFines(table);});});actions.getChildren().add(submit);}
        pageHost.getChildren().addAll(actions,table);VBox.setVgrow(table,Priority.ALWAYS);loadFines(table);
    }

    private void loadFines(TableView<Fine> table) {
        String path=session.role().equals("Administration")?"/api/admin/fines":"/api/student/fines?studentId="+api.encode(session.studentId());
        run(api.get(path,token()),data->table.setItems(FXCollections.observableArrayList(api.list(data,new TypeReference<List<Fine>>(){}))));
    }

    private void administratorsPage() {
        TableView<Administrator> table=table();col(table,"Name",Administrator::name);col(table,"Email",Administrator::email);col(table,"Staff role",Administrator::staffRole);
        Button refresh=new Button("Refresh");refresh.setOnAction(e->loadAdministrators(table));Button create=primaryButton("Create administrator");create.setOnAction(e->createAdministrator(table));
        pageHost.getChildren().addAll(new HBox(10,refresh,create),table);VBox.setVgrow(table,Priority.ALWAYS);loadAdministrators(table);
    }

    private void loadAdministrators(TableView<Administrator> table) {run(api.get("/api/super-admin/administrators",token()),data->table.setItems(FXCollections.observableArrayList(api.list(data,new TypeReference<List<Administrator>>(){}))));}

    private void registeredStudentsPage() {
        TableView<StudentSummary> table = table();
        col(table, "Name", StudentSummary::name); col(table, "Enrollment", StudentSummary::studentId);
        col(table, "Email", StudentSummary::email); col(table, "Membership status", StudentSummary::status);
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> run(api.get("/api/super-admin/students", token()), data -> table.setItems(
                FXCollections.observableArrayList(api.list(data, new TypeReference<List<StudentSummary>>() {})))));
        pageHost.getChildren().addAll(new Label("All student registrations and approval status."), refresh, table);
        VBox.setVgrow(table, Priority.ALWAYS); refresh.fire();
    }

    private void reportsPage() {
        Label intro = new Label("Live totals from the library API. Use Refresh after recording issues, returns, or fines.");
        intro.getStyleClass().add("muted"); pageHost.getChildren().add(intro);
        HBox totals = new HBox(14); pageHost.getChildren().add(totals);
        VBox books = statCard("Book titles", "Loading...");
        VBox students = statCard("Student members", "Loading...");
        VBox issues = statCard("Currently issued", "Loading...");
        VBox fines = statCard("Unpaid fines", "Loading...");
        totals.getChildren().addAll(books, students, issues, fines);
        Button refresh = primaryButton("Refresh report");
        refresh.setOnAction(event -> {
            for (int i = 0; i < totals.getChildren().size(); i++) {
                VBox card = (VBox) totals.getChildren().get(i);
                ((Label) card.getChildren().get(1)).setText("Loading...");
            }
            run(api.get("/api/books", token()), data -> ((Label) books.getChildren().get(1)).setText(Integer.toString(api.list(data, new TypeReference<List<Book>>() {}).size())));
            run(api.get("/api/admin/students?pendingOnly=false", token()), data -> ((Label) students.getChildren().get(1)).setText(Integer.toString(api.list(data, new TypeReference<List<Student>>() {}).size())));
            run(api.get("/api/admin/loans", token()), data -> ((Label) issues.getChildren().get(1)).setText(Long.toString(api.list(data, new TypeReference<List<Loan>>() {}).stream().filter(loan -> !loan.returned()).count())));
            run(api.get("/api/admin/fines", token()), data -> ((Label) fines.getChildren().get(1)).setText(Long.toString(api.list(data, new TypeReference<List<Fine>>() {}).stream().filter(fine -> !"PAID".equalsIgnoreCase(fine.status())).count())));
        });
        pageHost.getChildren().add(refresh); refresh.fire();
    }

    private void systemSettingsPage() {
        VBox card = new VBox(14); card.getStyleClass().add("stat-card");
        Label apiUrl = new Label("API endpoint: " + api.baseUrl());
        Label accountRole = new Label("Signed-in role: " + session.role());
        Label status = new Label("Checking API health...");
        Button refresh = primaryButton("Check connection");
        refresh.setOnAction(event -> run(api.get("/api/health", token()), data -> status.setText("API status: " + data.path("status").asText("Online"))));
        card.getChildren().addAll(new Label("System connection"), apiUrl, accountRole, status, refresh);
        pageHost.getChildren().addAll(new Label("Read-only connection settings for this desktop client."), card);
        refresh.fire();
    }

    private void scannerPage() {
        Label intro = new Label("Capture a book cover or choose a photo. OCR reads the title or printed book ID, then you can issue or return the matched catalogue book.");
        intro.getStyleClass().add("muted"); intro.setWrapText(true);
        ImageView preview = new ImageView(); preview.setFitWidth(520); preview.setFitHeight(300); preview.setPreserveRatio(true);
        StackPane previewPanel = new StackPane(preview, new Label("Camera or book photo preview"));
        previewPanel.setMinHeight(300); previewPanel.setMaxWidth(560); previewPanel.getStyleClass().add("stat-card");
        preview.imageProperty().addListener((obs, old, value) -> {
            if (value != null && !previewPanel.getChildren().isEmpty()) previewPanel.getChildren().removeIf(node -> node instanceof Label);
        });
        TextArea recognized = new TextArea(); recognized.setEditable(false); recognized.setWrapText(true); recognized.setPrefRowCount(5);
        recognized.setPromptText("OCR text will appear here for review.");
        TextField bookId = new TextField(); bookId.setPromptText("Catalogue book ID (you can correct this manually)");
        Label scanStatus = new Label("Allow camera access when Windows asks. You can also upload a clear photo."); scanStatus.getStyleClass().add("small-muted");
        Button camera = new Button("Capture from camera"); camera.getStyleClass().add("outline-button");
        camera.setOnAction(event -> captureFromCamera(preview, recognized, bookId, scanStatus, camera));
        Button upload = new Button("Choose image"); upload.getStyleClass().add("outline-button");
        upload.setOnAction(event -> {
            FileChooser chooser = new FileChooser(); chooser.setTitle("Choose a book image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Book images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tif", "*.tiff"));
            File selected = chooser.showOpenDialog(stage);
            if (selected != null) {
                try { preview.setImage(new Image(selected.toURI().toString())); }
                catch (Exception exception) { scanStatus.setText("Could not preview this image. Try PNG or JPEG."); return; }
                try { processScannedImage(ImageIO.read(selected), recognized, bookId, scanStatus, camera); }
                catch (Exception exception) { scanStatus.setText("Could not read the selected image."); }
            }
        });
        Button issue = primaryButton("Issue scanned book");
        issue.setOnAction(event -> {
            Long id = scannedBookId(bookId, scanStatus); if (id == null) return;
            issueBook(id, () -> showPage(session.role().equals("Student") ? "My issued books" : "Issue / return"));
        });
        Button giveBack = new Button("Return scanned book"); giveBack.getStyleClass().add("outline-button");
        giveBack.setOnAction(event -> { Long id = scannedBookId(bookId, scanStatus); if (id != null) returnScannedBook(id, scanStatus); });
        VBox card = new VBox(14, new HBox(10, camera, upload), previewPanel,
                labeled("OCR result", recognized), labeled("Matched book ID", bookId), scanStatus,
                new HBox(10, issue, giveBack));
        card.getStyleClass().add("stat-card"); card.setMaxWidth(700);
        pageHost.getChildren().addAll(intro, card);
    }

    private void captureFromCamera(ImageView preview, TextArea recognized, TextField bookId, Label status, Button cameraButton) {
        cameraButton.setDisable(true); status.setText("Opening the default camera...");
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam == null) throw new IllegalStateException("No camera was found. Choose an image or use a USB barcode scanner.");
            try {
                webcam.open();
                try { Thread.sleep(350); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                BufferedImage captured = webcam.getImage();
                if (captured == null) throw new IllegalStateException("The camera did not return an image.");
                return captured;
            } finally { if (webcam.isOpen()) webcam.close(); }
        }).whenComplete((captured, failure) -> Platform.runLater(() -> {
            cameraButton.setDisable(false);
            if (failure != null) { status.setText(message(failure)); return; }
            preview.setImage(toFxImage(captured));
            processScannedImage(captured, recognized, bookId, status, cameraButton);
        }));
    }

    private WritableImage toFxImage(BufferedImage source) {
        WritableImage target = new WritableImage(source.getWidth(), source.getHeight());
        int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
        PixelWriter writer = target.getPixelWriter();
        writer.setPixels(0, 0, source.getWidth(), source.getHeight(), PixelFormat.getIntArgbInstance(), pixels, 0, source.getWidth());
        return target;
    }

    private void processScannedImage(BufferedImage image, TextArea recognized, TextField bookId, Label status, Button cameraButton) {
        if (image == null) { status.setText("The selected file is not a supported image."); return; }
        status.setText("Reading the image with OCR..."); cameraButton.setDisable(true);
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            net.sourceforge.tess4j.Tesseract ocr = new net.sourceforge.tess4j.Tesseract();
            ocr.setLanguage("eng");
            try { return ocr.doOCR(image); }
            catch (net.sourceforge.tess4j.TesseractException exception) { throw new CompletionException(exception); }
        }).whenComplete((text, failure) -> Platform.runLater(() -> {
            cameraButton.setDisable(false);
            if (failure != null) { status.setText("OCR failed: " + message(failure)); return; }
            recognized.setText(text == null ? "" : text.trim());
            matchScannedBook(text == null ? "" : text, bookId, status);
        }));
    }

    private void matchScannedBook(String text, TextField bookId, Label status) {
        run(api.get("/api/books", token()), data -> {
            List<Book> books = api.list(data, new TypeReference<List<Book>>() {});
            String sourceText = text.toLowerCase(java.util.Locale.ROOT);
            String normalized = sourceText.replaceAll("[^a-z0-9]", "");
            Book match = books.stream().filter(book -> sourceText.matches("(?s).*\\b" + java.util.regex.Pattern.quote(Long.toString(book.id())) + "\\b.*"))
                    .findFirst().orElse(null);
            if (match == null) {
                match = books.stream().map(book -> Map.entry(book, book.title().toLowerCase().split("[^a-z0-9]+")))
                        .map(entry -> Map.entry(entry.getKey(), java.util.Arrays.stream(entry.getValue()).filter(word -> word.length() >= 3 && normalized.contains(word)).count()))
                        .filter(entry -> entry.getValue() >= 2).max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            }
            if (match == null) { status.setText("Text captured. Could not match a catalogue title or ID; enter the book ID manually."); return; }
            bookId.setText(Long.toString(match.id()));
            status.setText("Matched: " + match.title() + " by " + match.author() + ". Check the ID before continuing.");
        });
    }

    private Long scannedBookId(TextField field, Label status) {
        try { return Long.parseLong(field.getText().trim()); }
        catch (Exception exception) { status.setText("Scan a book or enter a valid catalogue book ID first."); return null; }
    }

    private void returnScannedBook(long bookId, Label status) {
        boolean studentOnly = session.role().equals("Student");
        String listPath = studentOnly ? "/api/student/loans?studentId=" + api.encode(session.studentId()) : "/api/admin/loans";
        api.get(listPath, token()).whenComplete((data, failure) -> Platform.runLater(() -> {
            if (failure != null) { status.setText(message(failure)); return; }
            List<Loan> activeLoans = api.list(data, new TypeReference<List<Loan>>() {}).stream()
                    .filter(loan -> loan.bookId() == bookId && !loan.returned()).toList();
            if (activeLoans.isEmpty()) { status.setText("No active issue record exists for this book."); return; }
            Loan selected = activeLoans.getFirst();
            if (activeLoans.size() > 1) {
                ChoiceDialog<Loan> choose = new ChoiceDialog<>(selected, activeLoans);
                choose.setTitle("Select issued copy"); choose.setHeaderText("Several active issues use this book ID.");
                choose.setContentText("Choose the borrower whose copy is being returned:");
                selected = choose.showAndWait().orElse(null);
                if (selected == null) return;
            }
            Loan loan = selected;
            String path = studentOnly ? "/api/student/loans/" + loan.id() + "/return" : "/api/admin/loans/" + loan.id() + "/return";
            api.post(path, Map.of(), token()).whenComplete((result, returnFailure) -> Platform.runLater(() -> {
                if (returnFailure != null) { status.setText(message(returnFailure)); return; }
                status.setText("Return recorded successfully."); notice("Book return recorded.");
            }));
        }));
    }

    private void profilePage() {
        ImageView photo = new ImageView(); photo.setFitWidth(104); photo.setFitHeight(104); photo.setPreserveRatio(true); photo.setClip(new Circle(52, 52, 52));
        Label details = new Label("Loading account details..."); details.setWrapText(true);
        Label feedback = new Label(); feedback.getStyleClass().add("notice"); feedback.setWrapText(true); feedback.setVisible(false); feedback.setManaged(false);
        TextField name = new TextField(session.name());
        PasswordField currentPassword = new PasswordField(); currentPassword.setPromptText("Current password (for password change)");
        PasswordField newPassword = new PasswordField(); newPassword.setPromptText("New password (leave blank to keep current)");
        Button upload = new Button("Upload profile photo"); upload.getStyleClass().add("outline-button");
        upload.setOnAction(event -> chooseProfilePhoto(photo));
        Button save = primaryButton("Save profile");
        save.setOnAction(event -> {
            if (name.getText().isBlank()) { feedback.setText("Name cannot be empty."); feedback.setVisible(true); feedback.setManaged(true); return; }
            boolean changingPassword = !newPassword.getText().isBlank();
            if (changingPassword && (currentPassword.getText().isBlank() || newPassword.getText().length() < 8)) {
                feedback.setText("Enter your current password and a new password with at least 8 characters."); feedback.setVisible(true); feedback.setManaged(true); return;
            }
            save.setDisable(true);
            String profilePath = session.role().equals("Student") ? "/api/student/profile" : "/api/auth/profile";
            Map<String, Object> profileChanges = session.role().equals("Student")
                    ? ApiClient.data("name", name.getText().trim())
                    : ApiClient.data("name", name.getText().trim(),
                            "currentPassword", changingPassword ? currentPassword.getText() : null,
                            "newPassword", changingPassword ? newPassword.getText() : null);
            java.util.concurrent.CompletableFuture<JsonNode> update = api.put(profilePath, profileChanges, token());
            if (session.role().equals("Student") && changingPassword) {
                update = update.thenCompose(ignored -> api.put("/api/auth/profile", ApiClient.data("name", name.getText().trim(),
                        "currentPassword", currentPassword.getText(), "newPassword", newPassword.getText()), token()));
            }
            run(update, data -> {
                session = new Session(session.token(), session.role(), name.getText().trim(), session.studentId());
                feedback.setText("Profile updated successfully."); feedback.setVisible(true); feedback.setManaged(true);
                currentPassword.clear(); newPassword.clear(); shell.setTop(topbar()); save.setDisable(false);
            });
            save.setDisable(false);
        });
        HBox photoRow = new HBox(18, photo, new VBox(10, upload, details)); photoRow.setAlignment(Pos.CENTER_LEFT);
        VBox editor = new VBox(14, new Label("Account information"), labeled("Full name", name),
                labeled("Current password", currentPassword), labeled("New password", newPassword), save, feedback);
        editor.getStyleClass().add("stat-card"); editor.setMaxWidth(620);
        pageHost.getChildren().addAll(photoRow, editor);
        run(api.get("/api/auth/profile", token()), data -> {
            details.setText(data.path("email").asText() + "\n" + data.path("role").asText()
                    + (data.path("studentId").asText().isBlank() ? "" : " · " + data.path("studentId").asText()));
            loadProfilePhoto(data.path("email").asText(), photo);
        });
        if (session.role().equals("Student")) {
            run(api.get("/api/student/profile", token()), data -> {
                VBox academic = new VBox(8); academic.getStyleClass().add("stat-card");
                academic.getChildren().addAll(new Label("Student details"),
                        new Label("Enrollment: " + data.path("studentId").asText()),
                        new Label("Department: " + data.path("department").asText()),
                        new Label("Course: " + data.path("course").asText()),
                        new Label("Year / semester: " + data.path("year").asText() + " · " + data.path("semester").asText()),
                        new Label("Father's name: " + data.path("fatherName").asText()),
                        new Label("Gender: " + data.path("gender").asText()),
                        new Label("Mobile: " + data.path("mobileNumber").asText()),
                        new Label("Address: " + data.path("address").asText() + ", " + data.path("district").asText() + ", " + data.path("state").asText()),
                        new Label("Membership: " + data.path("status").asText()));
                Button editDetails = new Button("Edit student details"); editDetails.getStyleClass().add("outline-button");
                editDetails.setOnAction(event -> editStudentDetails(data));
                academic.getChildren().add(editDetails);
                pageHost.getChildren().add(2, academic);
            });
        }
    }

    private void editStudentDetails(JsonNode current) {
        TextField name = new TextField(current.path("name").asText());
        TextField father = new TextField(current.path("fatherName").asText());
        ComboBox<String> gender = options("Select gender", "Male", "Female", "Other", "Non-binary", "Prefer not to say");
        gender.setValue(current.path("gender").asText());
        ComboBox<String> department = options("Select department", ACADEMIC_OPTIONS.keySet().stream().sorted().toArray(String[]::new));
        department.setValue(current.path("department").asText());
        ComboBox<String> course = options("Select course");
        java.util.function.Consumer<String> loadCourses = selected -> {
            List<String> courses = new ArrayList<>(ACADEMIC_OPTIONS.getOrDefault(selected, List.of()));
            courses.addAll(COMMON_COURSES); courses.add("Other / not listed");
            course.getItems().setAll(courses.stream().distinct().toList());
        };
        loadCourses.accept(current.path("department").asText());
        course.setValue(current.path("course").asText());
        department.valueProperty().addListener((obs, old, value) -> { loadCourses.accept(value); course.setValue(null); });
        ComboBox<String> year = options("Select year", "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "Research Scholar");
        year.setValue(current.path("year").asText());
        ComboBox<String> semester = options("Select semester", "Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8", "Semester 9", "Semester 10", "Not applicable");
        semester.setValue(current.path("semester").asText());
        TextField address = new TextField(current.path("address").asText());
        ComboBox<String> state = options("Select state / UT", INDIA_LOCATIONS.keySet().stream().sorted().toArray(String[]::new));
        state.setValue(current.path("state").asText());
        ComboBox<String> district = options("Select district"); district.setEditable(true);
        java.util.function.Consumer<String> loadDistricts = selected -> {
            List<String> districts = new ArrayList<>(INDIA_LOCATIONS.getOrDefault(selected, List.of()));
            if (!districts.contains("Other / not listed")) districts.add("Other / not listed");
            district.getItems().setAll(districts);
        };
        loadDistricts.accept(current.path("state").asText());
        district.setValue(current.path("district").asText()); district.getEditor().setText(current.path("district").asText());
        state.valueProperty().addListener((obs, old, value) -> { loadDistricts.accept(value); district.setValue(null); district.getEditor().clear(); });
        TextField aadhaar = new TextField(current.path("aadhaarId").asText());
        TextField mobile = new TextField(current.path("mobileNumber").asText());
        CheckBox consent = new CheckBox("I consent to use my details for library membership.");
        consent.setSelected(current.path("shareDataPermission").asBoolean());
        VBox fields = new VBox(12,
                row(field("Full name", name), field("Father's name", father)),
                row(field("Gender", gender), field("Department", department)),
                row(field("Course", course), field("Year", year)),
                row(field("Semester", semester), field("Mobile number", mobile)),
                field("Address", address), row(field("State / UT", state), field("District", district)),
                field("Aadhaar ID", aadhaar), consent);
        ScrollPane scroll = new ScrollPane(fields); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(520);
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Edit student profile");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK); dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait().filter(result -> result == ButtonType.OK).ifPresent(result -> {
            if (name.getText().isBlank() || !mobile.getText().matches("\\d{10}") || !aadhaar.getText().matches("\\d{12}")) {
                notice("Enter a name, 10-digit mobile number, and 12-digit Aadhaar ID."); return;
            }
            if (department.getValue() == null || course.getValue() == null || state.getValue() == null || districtValue(district).isBlank()) {
                notice("Complete department, course, state, and district first."); return;
            }
            if (!consent.isSelected()) { notice("Consent is required to save student profile details."); return; }
            run(api.put("/api/student/profile", ApiClient.data("name", name.getText().trim(),
                    "fatherName", father.getText().trim(), "gender", gender.getValue(),
                    "department", department.getValue(), "course", course.getValue(),
                    "year", year.getValue(), "semester", semester.getValue(), "address", address.getText().trim(),
                    "state", state.getValue(), "district", districtValue(district), "aadhaarId", aadhaar.getText().trim(),
                    "mobileNumber", mobile.getText().trim(), "shareDataPermission", true), token()),
                    updated -> { session = new Session(session.token(), session.role(), name.getText().trim(), session.studentId()); shell.setTop(topbar()); showPage("My profile"); });
        });
    }

    private void chooseProfilePhoto(ImageView view) {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Choose profile photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File selected = chooser.showOpenDialog(stage); if (selected == null) return;
        api.get("/api/auth/profile", token()).thenAccept(profile -> Platform.runLater(() -> {
            try {
                String email = profile.path("email").asText("account").toLowerCase().replaceAll("[^a-z0-9._-]", "_");
                Path directory = Path.of(System.getProperty("user.home"), ".bbau-library", "profiles");
                Files.createDirectories(directory);
                String extension = selected.getName().toLowerCase().endsWith(".png") ? ".png" : ".jpg";
                Path destination = directory.resolve(email + extension);
                Files.copy(selected.toPath(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                view.setImage(new Image(destination.toUri().toString()));
                notice("Profile photo saved for this account on this computer.");
            } catch (Exception exception) { notice("Could not save the selected profile photo."); }
        })).exceptionally(failure -> { Platform.runLater(() -> notice(message(failure))); return null; });
    }

    private void loadProfilePhoto(String email, ImageView view) {
        if (email == null || email.isBlank()) return;
        String safe = email.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        Path directory = Path.of(System.getProperty("user.home"), ".bbau-library", "profiles");
        for (String extension : List.of(".png", ".jpg")) {
            Path photo = directory.resolve(safe + extension);
            if (Files.exists(photo)) { view.setImage(new Image(photo.toUri().toString())); return; }
        }
    }

    private void addBook(TableView<Book> table) {
        TextField title=new TextField();TextField author=new TextField();TextField category=new TextField();TextField copies=new TextField();
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Add catalogue book");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        dialog.getDialogPane().setContent(new VBox(10,labeled("Title",title),labeled("Author",author),labeled("Category",category),labeled("Copies",copies)));
        dialog.showAndWait().filter(x->x==ButtonType.OK).ifPresent(x->{try{run(api.post("/api/admin/books",ApiClient.data("title",title.getText().trim(),"author",author.getText().trim(),"category",category.getText().trim(),"copies",Integer.parseInt(copies.getText().trim())),token()),d->{notice("Book added to catalogue.");loadBooks(table);});}catch(NumberFormatException ex){notice("Copies must be a whole number.");}});
    }

    private void deleteBook(Book book,TableView<Book> table) {run(api.delete("/api/admin/books?title="+api.encode(book.title()),token()),d->{notice("Book removed from catalogue.");loadBooks(table);});}

    private void addFine(TableView<Fine> table) {
        TextField student=new TextField();TextField reason=new TextField();TextField amount=new TextField();
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Record a fine");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        dialog.getDialogPane().setContent(new VBox(10,labeled("Student ID",student),labeled("Reason",reason),labeled("Amount (₹)",amount)));
        dialog.showAndWait().filter(x->x==ButtonType.OK).ifPresent(x->{try{run(api.post("/api/admin/fines",ApiClient.data("studentId",student.getText().trim(),"reason",reason.getText().trim(),"amount",Integer.parseInt(amount.getText().trim())),token()),d->{notice("Fine recorded.");loadFines(table);});}catch(NumberFormatException ex){notice("Fine amount must be a whole number.");}});
    }

    private void createAdministrator(TableView<Administrator> table) {
        TextField name=new TextField();TextField email=new TextField();PasswordField password=new PasswordField();ComboBox<String> role=new ComboBox<>(FXCollections.observableArrayList("Librarian","Library staff"));role.setValue("Librarian");
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Create Administration account");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        dialog.getDialogPane().setContent(new VBox(10,labeled("Name",name),labeled("Email",email),labeled("Staff role",role),labeled("Initial password",password)));
        dialog.showAndWait().filter(x->x==ButtonType.OK).ifPresent(x->run(api.post("/api/super-admin/administrators",ApiClient.data("name",name.getText().trim(),"email",email.getText().trim(),"staffRole",role.getValue(),"password",password.getText()),token()),d->{notice("Administrator account created.");loadAdministrators(table);}));
    }

    private <T> TableView<T> table() {TableView<T> table=new TableView<>();table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);table.setPlaceholder(new Label("No records to display."));return table;}
    private <T> void col(TableView<T> table,String title,Function<T,String> value) {TableColumn<T,String> column=new TableColumn<>(title);column.setCellValueFactory(c->new ReadOnlyStringWrapper(value.apply(c.getValue())));table.getColumns().add(column);}

    private VBox statCard(String title,String value) {Label a=new Label(title.toUpperCase());a.getStyleClass().add("eyebrow");Label b=new Label(value);b.getStyleClass().add("stat-value");VBox card=new VBox(12,a,b);card.getStyleClass().add("stat-card");card.setMinWidth(210);return card;}
    private VBox labeled(String label,Node input) {Label name=new Label(label);name.getStyleClass().add("field-label");VBox group=new VBox(6,name,input);group.setMaxWidth(Double.MAX_VALUE);return group;}
    private Label heading(String text) {Label label=new Label(text);label.getStyleClass().add("page-title");return label;}
    private Button primaryButton(String text) {Button button=new Button(text);button.getStyleClass().add("primary-button");return button;}
    private String token() {return session==null?null:session.token();}
    private String blank(String value,String fallback) {return value==null||value.isBlank()?fallback:value;}

    private void run(java.util.concurrent.CompletableFuture<JsonNode> call,Consumer<JsonNode> success) {
        call.whenComplete((data,failure)->Platform.runLater(()->{if(failure!=null)notice(message(failure));else success.accept(data);}));
    }

    private void notice(String text) {if(notice!=null){notice.setText(text);notice.setVisible(true);notice.setManaged(true);}}
    private String message(Throwable failure) {
        Throwable root=failure;while((root instanceof CompletionException||root instanceof java.util.concurrent.ExecutionException)&&root.getCause()!=null)root=root.getCause();
        if(root instanceof ApiClient.ApiException)return root.getMessage();
        return "Cannot reach the Library API at "+api.baseUrl()+". Start the backend or set LIBRARY_API_URL.";
    }
    private StackPane centered(Node node) {StackPane pane=new StackPane(node);pane.setPadding(new Insets(30));pane.getStyleClass().add("auth-background");return pane;}
    private String css() {return getClass().getResource("/ac/bbau/library/desktop/theme.css").toExternalForm();}

    public record Session(String token,String role,String name,String studentId) {}
    public record Book(long id,String title,String author,String category,int copies,int available) {}
    public record Student(long id,String name,String studentId,String email,String status) {}
    public record StudentSummary(long id,String name,String studentId,String email,String status) {}
    public record Loan(long id,long bookId,String studentId,String dueDate,boolean returned) {
        @Override public String toString() { return studentId + " · due " + dueDate; }
    }
    public record Fine(long id,String studentId,String reason,int amount,String status) {}
    public record Administrator(long id,String name,String email,String staffRole) {}
}
