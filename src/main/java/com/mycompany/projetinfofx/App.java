package com.mycompany.projetinfofx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class App extends Application {
    private ObservableList<Equipement> equipements = FXCollections.observableArrayList();
    private ObservableList<Machine> machines = FXCollections.observableArrayList();
    private ObservableList<Poste> postes = FXCollections.observableArrayList();
    private ObservableList<Operateur> operateurs = FXCollections.observableArrayList();
    private ObservableList<Gamme> gammes = FXCollections.observableArrayList();

    private ListView<Machine> machineListView;
    private ListView<Poste> posteListView;
    private ListView<Operateur> operListView;
    private ListView<String> eventsListView;
    private ListView<String> maintenanceListView;
    private Canvas atelierCanvas;
    private File maintenanceFile;

    private ListView<Gamme> gammeListView;
    private ListView<Operation> operationListView;
    private Label lblGammeCost;
    private Label lblGammeDuration;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Label title = new Label("Atelier de Fabrication");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold;");

        Button btnAddMachine = new Button("Ajouter Machine");
        btnAddMachine.setOnAction(e -> showAddMachineDialog());
        Button btnAddPoste = new Button("Ajouter Poste");
        btnAddPoste.setOnAction(e -> showAddPosteDialog());
        Button btnAddOper = new Button("Ajouter Opérateur");
        btnAddOper.setOnAction(e -> showAddOperateurDialog());
        Button btnLoadFile = new Button("Charger Maintenance");
        btnLoadFile.setOnAction(e -> selectMaintenanceFile());
        Button btnAddEvent = new Button("Ajouter Événement");
        btnAddEvent.setOnAction(e -> showAddEventDialog());

        HBox topBar = new HBox(10, title, btnAddMachine, btnAddPoste, btnAddOper, btnLoadFile, btnAddEvent);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Équipements", createEquipementPane()),
            new Tab("Opérateurs", createOperateurPane()),
            new Tab("Gammes", createGammesPane()),
            new Tab("Maintenance", createMaintenancePane()),
            new Tab("Atelier", createAtelierPane())
        );

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 1000, 750);
        stage.setTitle("Gestion Atelier JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createEquipementPane() {
        VBox v = new VBox(10);
        v.setPadding(new Insets(10));
        machineListView = new ListView<>(machines);
        posteListView = new ListView<>(postes);
        v.getChildren().addAll(
            new Label("Machines"), machineListView,
            new Label("Postes"), posteListView
        );
        return v;
    }

    private VBox createOperateurPane() {
        VBox v = new VBox(10);
        v.setPadding(new Insets(10));
        operListView = new ListView<>(operateurs);
        v.getChildren().addAll(new Label("Opérateurs"), operListView);
        return v;
    }

    private BorderPane createGammesPane() {
        BorderPane pane = new BorderPane(); pane.setPadding(new Insets(10));
        VBox left = new VBox(10);
        TextField tfRefG = new TextField(); tfRefG.setPromptText("Réf Gamme");
        Button btnNewG = new Button("Créer Gamme");
        gammeListView = new ListView<>(gammes);
        btnNewG.setOnAction(e -> {
            String ref = tfRefG.getText().trim();
            if (!ref.isEmpty()) {
                gammes.add(new Gamme(ref));
                tfRefG.clear();
            }
        });
        left.getChildren().addAll(new Label("Gammes"), tfRefG, btnNewG, gammeListView);
        left.setPrefWidth(200);

        VBox center = new VBox(10); center.setPadding(new Insets(0,10,0,10));
        operationListView = new ListView<>();
        TextField tfOpRef = new TextField(); tfOpRef.setPromptText("Réf Op");
        TextField tfOpDes = new TextField(); tfOpDes.setPromptText("Désignation");
        TextField tfOpDur = new TextField(); tfOpDur.setPromptText("Durée (h)");
        ComboBox<Equipement> cbEquip = new ComboBox<>(equipements);
        cbEquip.getSelectionModel().selectFirst();
        Button btnAddOp = new Button("Ajouter Opération");
        btnAddOp.setOnAction(e -> {
            Gamme g = gammeListView.getSelectionModel().getSelectedItem();
            Equipement eq = cbEquip.getValue();
            try {
                float dur = Float.parseFloat(tfOpDur.getText());
                Operation op = new Operation(tfOpRef.getText(), tfOpDes.getText(), eq, dur);
                if (g != null) {
                    g.creerGamme(op);
                    operationListView.getItems().add(op);
                    updateGammeStats(g);
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Durée invalide").showAndWait();
            }
        });
        lblGammeCost = new Label("Coût: 0"); lblGammeDuration = new Label("Durée: 0");
        Button btnStartGamme = new Button("Démarrer Gamme");
        btnStartGamme.setOnAction(ev -> {
            Gamme selG = gammeListView.getSelectionModel().getSelectedItem();
            if (selG != null) startGamme(selG);
        });
        center.getChildren().addAll(
            new Label("Opérations"), operationListView,
            new HBox(10, tfOpRef, tfOpDes, tfOpDur, cbEquip, btnAddOp),
            new HBox(20, lblGammeCost, lblGammeDuration),
            btnStartGamme
        );
        gammeListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            operationListView.getItems().clear();
            if (sel != null) {
                operationListView.getItems().addAll(sel.getOperations());
                updateGammeStats(sel);
            }
        });
        pane.setLeft(left); pane.setCenter(center);
        return pane;
    }

    private VBox createMaintenancePane() {
        VBox v = new VBox(10); v.setPadding(new Insets(10));
        eventsListView = new ListView<>();
        maintenanceListView = new ListView<>();
        v.getChildren().addAll(
            new Label("Journal des événements"), eventsListView,
            new Label("Fiabilité des machines"), maintenanceListView
        ); return v;
    }

    private StackPane createAtelierPane() {
        atelierCanvas = new Canvas(900,600);
        drawMachines(atelierCanvas.getGraphicsContext2D());
        StackPane sp = new StackPane(atelierCanvas); sp.setPadding(new Insets(10)); return sp;
    }

    private void showAddMachineDialog() {
        Dialog<Machine>d=new Dialog<>();d.setTitle("Ajouter Machine");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(10);g.setPadding(new Insets(20));
        TextField r=new TextField(),dsg=new TextField(),t=new TextField();
        TextField c=new TextField(),x=new TextField(),y=new TextField();
        g.addRow(0,new Label("Réf:"),r);g.addRow(1,new Label("Désignation:"),dsg);
        g.addRow(2,new Label("Type:"),t);g.addRow(3,new Label("Coût h:"),c);
        g.addRow(4,new Label("X:"),x);g.addRow(5,new Label("Y:"),y);
        d.getDialogPane().setContent(g);d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{if(bt==ButtonType.OK){try{Machine m=new Machine(r.getText(),dsg.getText(),t.getText(),
            Float.parseFloat(c.getText()),Float.parseFloat(x.getText()),Float.parseFloat(y.getText()));
            machines.add(m);equipements.add(m);drawMachines(atelierCanvas.getGraphicsContext2D());return m;}catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Valeur invalide").showAndWait();}}return null;});d.showAndWait();}

    private void showAddPosteDialog() {
        Dialog<Poste>d=new Dialog<>();d.setTitle("Ajouter Poste");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(10);g.setPadding(new Insets(20));
        TextField r=new TextField();r.setPromptText("Réf Poste");
        ListView<Machine> lv=new ListView<>(machines);lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        g.addRow(0,new Label("Réf Poste:"),r);g.addRow(1,new Label("Machines:"),lv);
        d.getDialogPane().setContent(g);d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.setResultConverter(bt->{if(bt==ButtonType.OK){Poste p=new Poste(r.getText(),r.getText(),lv.getSelectionModel().getSelectedItems());
            postes.add(p);equipements.add(p);return p;}return null;});d.showAndWait();}

    private void showAddOperateurDialog() {
        Dialog<Operateur>d=new Dialog<>();d.setTitle("Ajouter Opérateur");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(10);g.setPadding(new Insets(20));
        TextField code=new TextField(),nom=new TextField(),pren=new TextField();
        code.setPromptText("Code");nom.setPromptText("Nom");pren.setPromptText("Prénom");
        g.addRow(0,new Label("Code (#):"),code);g.addRow(1,new Label("Nom:"),nom);g.addRow(2,new Label("Prénom:"),pren);
        d.getDialogPane().setContent(g);d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.setResultConverter(bt->{if(bt==ButtonType.OK){String cd="OP"+code.getText().trim();Operateur o=new Operateur(cd,nom.getText(),pren.getText(),new java.util.ArrayList<>());
            operateurs.add(o);return o;}return null;});d.showAndWait();}

    private void showAddEventDialog() {
        if(maintenanceFile==null){FileChooser fc=new FileChooser();fc.setTitle("Créer Maintenance");maintenanceFile=fc.showSaveDialog(null);
            if(maintenanceFile==null) return;try(FileWriter fw=new FileWriter(maintenanceFile,false)){}catch(IOException ignored){}
        }
        Dialog<Void>d=new Dialog<>();d.setTitle("Ajouter Événement");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(10);g.setPadding(new Insets(20));
        DatePicker dp=new DatePicker(LocalDate.now());TextField tf=new TextField(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString());
        ComboBox<Equipement> cbE=new ComboBox<>(equipements);cbE.getSelectionModel().selectFirst();
        ComboBox<String> cbT=new ComboBox<>(FXCollections.observableArrayList("A","D"));cbT.getSelectionModel().selectFirst();
        ComboBox<String> cbC=new ComboBox<>(FXCollections.observableArrayList("panne","maintenance","accident","ok"));cbC.getSelectionModel().selectFirst();
        ComboBox<Operateur> cbO=new ComboBox<>(operateurs);cbO.getSelectionModel().selectFirst();
        g.addRow(0,new Label("Date"),dp);g.addRow(1,new Label("Heure"),tf);g.addRow(2,new Label("Équip"),cbE);
        g.addRow(3,new Label("Type (A/D)"),cbT);g.addRow(4,new Label("Cause"),cbC);g.addRow(5,new Label("Opérateur"),cbO);
        d.getDialogPane().setContent(g);d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        d.setResultConverter(bt->{if(bt==ButtonType.OK){String line=String.join(";",
            dp.getValue().format(DateTimeFormatter.ofPattern("ddMMyyyy")),tf.getText(),cbE.getValue().getRef(),
            cbT.getValue(),cbO.getValue().getCode(),cbC.getValue());
            try(FileWriter fw=new FileWriter(maintenanceFile,true)){fw.write(line+System.lineSeparator());}
            catch(IOException ex){new Alert(Alert.AlertType.ERROR,"Échec écriture").showAndWait();}
            loadEventsAndFiability();return null;}return null;});d.showAndWait();}

    private void selectMaintenanceFile() {FileChooser fc=new FileChooser();fc.setTitle("Charger Maintenance");maintenanceFile=fc.showOpenDialog(null);if(maintenanceFile!=null)loadEventsAndFiability();}

    private void loadEventsAndFiability() {
        eventsListView.getItems().clear();
        try(BufferedReader br=new BufferedReader(new FileReader(maintenanceFile))){String l;while((l=br.readLine())!=null)eventsListView.getItems().add(l);
            Map<String,Float> fi=MaintenanceService.calculerFiabilite(maintenanceFile.getAbsolutePath());
            maintenanceListView.getItems().clear();fi.entrySet().stream().sorted((e1,e2)->Float.compare(e2.getValue(),e1.getValue()))
              .forEach(e->maintenanceListView.getItems().add(e.getKey()+" : "+String.format("%.2f%%",e.getValue())));
        }catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Erreur lecture").showAndWait();}
    }

    private void updateGammeStats(Gamme g) {lblGammeCost.setText(String.format("Coût: %.2f",g.coutGamme()));lblGammeDuration.setText(String.format("Durée: %.2f",g.dureeGamme()));}

    private void startGamme(Gamme g) {
        for(Operation op : g.getOperations()) {
            Equipement eq = op.getEquipement();
            if(eq instanceof Machine) {
                Machine m = (Machine)eq;
                m.setAvailable(false);
                if(!m.isAutomatic()) {
                    ObservableList<Operateur> freeOps = operateurs.filtered(o->!o.isBusy());
                    if(freeOps.isEmpty()) {
                        new Alert(Alert.AlertType.WARNING,"Aucun opérateur libre pour " + m.getRef()).showAndWait();
                    } else {
                        ChoiceDialog<Operateur> cd = new ChoiceDialog<>(freeOps.get(0), freeOps);
                        cd.setTitle("Affectation Opérateur");
                        cd.setHeaderText("Choisissez un opérateur pour " + m.getRef());
                        cd.setContentText("Opérateur:");
                        cd.showAndWait().ifPresent(o->o.setBusy(true));
                    }
                }
            }
        }
        machineListView.refresh(); operListView.refresh(); drawMachines(atelierCanvas.getGraphicsContext2D());
    }

    private void drawMachines(GraphicsContext gc) {
        gc.clearRect(0,0,atelierCanvas.getWidth(),atelierCanvas.getHeight());
        equipements.forEach(eq->{
            if(eq instanceof Machine) {
                Machine m=(Machine)eq;
                gc.fillOval(m.getX(),m.getY(),20,20);
                gc.fillText(m.getRef(),m.getX(),m.getY()-5);
            } else if(eq instanceof Poste) {
                Poste p=(Poste)eq;
                gc.strokeRect(10,10,100,50);
                gc.fillText(p.getRef(),15,25);
            }
        });
    }
}
