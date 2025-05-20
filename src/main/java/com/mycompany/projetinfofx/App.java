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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class App extends Application {

    // Données métier
    private final ObservableList<Equipement> equipements = FXCollections.observableArrayList();
    private final ObservableList<Machine>   machines    = FXCollections.observableArrayList();
    private final ObservableList<Poste>     postes      = FXCollections.observableArrayList();
    private final ObservableList<Operateur> operateurs  = FXCollections.observableArrayList();
    private final ObservableList<Gamme>     gammes      = FXCollections.observableArrayList();

    // Composants UI
    private ListView<Machine>    machineListView;
    private ListView<Poste>      posteListView;
    private ListView<Operateur>  operListView;
    private ListView<Gamme>      gammeListView;
    private ListView<Operation>  operationListView;
    private ListView<String>     eventsListView;
    private ListView<String>     maintenanceListView;
    private Canvas               atelierCanvas;
    private File                 maintenanceFile;
    private Label                lblGammeCost;
    private Label                lblGammeDuration;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        maintenanceFile = null;

        Label title = new Label("Atelier de Fabrication");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold;");
        Button btnAddMachine = new Button("Ajouter Machine");
        Button btnAddPoste   = new Button("Ajouter Poste");
        Button btnAddOper    = new Button("Ajouter Opérateur");
        Button btnLoadFile   = new Button("Charger Maintenance");
        Button btnAddEvent   = new Button("Ajouter Événement");

        btnAddMachine.setOnAction(e -> showAddMachineDialog());
        btnAddPoste .setOnAction(e -> showAddPosteDialog());
        btnAddOper  .setOnAction(e -> showAddOperateurDialog());
        btnLoadFile .setOnAction(e -> selectMaintenanceFile());
        btnAddEvent .setOnAction(e -> showAddEventDialog());

        HBox topBar = new HBox(10,
            title, btnAddMachine, btnAddPoste, btnAddOper, btnLoadFile, btnAddEvent
        );
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Équipements", createEquipementPane()),
            new Tab("Opérateurs",  createOperateurPane()),
            new Tab("Gammes",      createGammesPane()),
            new Tab("Maintenance", createMaintenancePane()),
            new Tab("Atelier",     createAtelierPane())
        );

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 1000, 750));
        stage.setTitle("Gestion Atelier JavaFX");
        stage.show();

        loadEventsAndFiability();
    }

    // === PANE ÉQUIPEMENTS ===
    private VBox createEquipementPane() {
        machineListView = new ListView<>(machines);
        posteListView   = new ListView<>(postes);
        machineListView.setCellFactory(lv -> new MachineCell());
        posteListView  .setCellFactory(lv -> new PosteCell());
        return new VBox(10,
            new Label("Machines"), machineListView,
            new Label("Postes"),   posteListView
        ) {{ setPadding(new Insets(10)); }};
    }

    // === PANE OPÉRATEURS ===
    private VBox createOperateurPane() {
        operListView = new ListView<>(operateurs);
        operListView.setCellFactory(lv -> new OperateurCell());
        return new VBox(10,
            new Label("Opérateurs"), operListView
        ) {{ setPadding(new Insets(10)); }};
    }

    // === PANE GAMMES ===
    private BorderPane createGammesPane() {
        gammeListView     = new ListView<>(gammes);
        operationListView = new ListView<>();
        lblGammeCost      = new Label("Coût: 0");
        lblGammeDuration  = new Label("Durée: 0");
        gammeListView.setCellFactory(lv -> new GammeCell());
        operationListView.setCellFactory(lv -> new OperationCell());

        // gauche : gestion gammes
        VBox left = new VBox(10);
        left.setPadding(new Insets(10));
        TextField tfRefG = new TextField(); tfRefG.setPromptText("Réf Gamme");
        Button btnNewG = new Button("Créer Gamme");
        btnNewG.setOnAction(e -> {
            String ref = tfRefG.getText().trim();
            if (!ref.isEmpty()) {
                gammes.add(new Gamme(ref));
                tfRefG.clear();
            }
        });
        left.getChildren().addAll(new Label("Gammes"), tfRefG, btnNewG, gammeListView);
        left.setPrefWidth(200);

        // centre : opérations + stats
        VBox center = new VBox(10);
        center.setPadding(new Insets(10));
        TextField tfOpRef = new TextField(); tfOpRef.setPromptText("Réf Op");
        TextField tfOpDes = new TextField(); tfOpDes.setPromptText("Désignation");
        TextField tfOpDur = new TextField(); tfOpDur.setPromptText("Durée (h)");
        ComboBox<Machine> cbMach = new ComboBox<>(machines);
        cbMach.getSelectionModel().selectFirst();
        Button btnAddOp = new Button("Ajouter Opération");
        btnAddOp.setOnAction(e -> {
            Gamme g = gammeListView.getSelectionModel().getSelectedItem();
            if (g != null) {
                try {
                    float dur = Float.parseFloat(tfOpDur.getText().trim());
                    Operation op = new Operation(
                        tfOpRef.getText().trim(),
                        tfOpDes.getText().trim(),
                        cbMach.getValue(),
                        dur
                    );
                    g.creerGamme(op);
                    operationListView.getItems().add(op);
                    updateGammeStats(g);
                    tfOpRef.clear(); tfOpDes.clear(); tfOpDur.clear();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Durée invalide").showAndWait();
                }
            }
        });
        Button btnStart = new Button("Démarrer Gamme");
        btnStart.setOnAction(e -> {
            Gamme g = gammeListView.getSelectionModel().getSelectedItem();
            if (g != null) startGamme(g);
        });

        center.getChildren().addAll(
            new Label("Opérations"), operationListView,
            new HBox(10, tfOpRef, tfOpDes, tfOpDur, cbMach, btnAddOp),
            new HBox(20, lblGammeCost, lblGammeDuration),
            btnStart
        );

        gammeListView.getSelectionModel().selectedItemProperty().addListener((obs,oldv,newv) -> {
            operationListView.getItems().clear();
            if (newv != null) {
                operationListView.getItems().addAll(newv.getOperations());
                updateGammeStats(newv);
            }
        });

        return new BorderPane(center, null, null, null, left);
    }

    // === PANE MAINTENANCE ===
    private VBox createMaintenancePane() {
        eventsListView      = new ListView<>();
        maintenanceListView = new ListView<>();
        return new VBox(10,
            new Label("Journal des événements"), eventsListView,
            new Label("Fiabilité"), maintenanceListView
        ) {{ setPadding(new Insets(10)); }};
    }

    // === PANE ATELIER ===
    private StackPane createAtelierPane() {
        atelierCanvas = new Canvas(900, 600);
        drawMachines(atelierCanvas.getGraphicsContext2D());
        return new StackPane(atelierCanvas) {{ setPadding(new Insets(10)); }};
    }

    // === DIALOGS D'AJOUT ===
    private void showAddMachineDialog() {
        Dialog<Machine> d = new Dialog<>();
        d.setTitle("Ajouter Machine");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef  = new TextField();
        TextField tfDes  = new TextField();
        TextField tfType = new TextField();
        TextField tfCost = new TextField();
        TextField tfX    = new TextField();
        TextField tfY    = new TextField();
        CheckBox cbAuto  = new CheckBox("Automatique");
        g.addRow(0, new Label("Réf:"), tfRef);
        g.addRow(1, new Label("Désignation:"), tfDes);
        g.addRow(2, new Label("Type:"), tfType);
        g.addRow(3, new Label("Coût h:"), tfCost);
        g.addRow(4, new Label("X:"), tfX);
        g.addRow(5, new Label("Y:"), tfY);
        g.addRow(6, cbAuto);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Machine m = new Machine(
                        tfRef.getText().trim(),
                        tfDes.getText().trim(),
                        tfType.getText().trim(),
                        Float.parseFloat(tfCost.getText().trim()),
                        Float.parseFloat(tfX.getText().trim()),
                        Float.parseFloat(tfY.getText().trim())
                    );
                    m.setAutomatic(cbAuto.isSelected());
                    machines.add(m);
                    equipements.add(m);
                    drawMachines(atelierCanvas.getGraphicsContext2D());
                    return m;
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Valeur invalide").showAndWait();
                }
            }
            return null;
        });
        d.showAndWait();
    }

    private void showAddPosteDialog() {
        Dialog<Poste> d = new Dialog<>();
        d.setTitle("Ajouter Poste");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef = new TextField();
        tfRef.setPromptText("Réf Poste");
        ListView<Machine> lv = new ListView<>(machines);
        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        g.addRow(0, new Label("Réf Poste:"), tfRef);
        g.addRow(1, new Label("Machines:"), lv);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Poste p = new Poste(
                    tfRef.getText().trim(),
                    tfRef.getText().trim(),
                    lv.getSelectionModel().getSelectedItems()
                );
                postes.add(p);
                equipements.add(p);
                return p;
            }
            return null;
        });
        d.showAndWait();
    }

    private void showAddOperateurDialog() {
        Dialog<Operateur> d = new Dialog<>();
        d.setTitle("Ajouter Opérateur");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfNum  = new TextField();
        TextField tfNom  = new TextField();
        TextField tfPren = new TextField();
        ComboBox<Equipement> cbAssign = new ComboBox<>(equipements);
        cbAssign.getSelectionModel().selectFirst();
        g.addRow(0, new Label("Num OP:"), tfNum);
        g.addRow(1, new Label("Nom:"), tfNom);
        g.addRow(2, new Label("Prénom:"), tfPren);
        g.addRow(3, new Label("Affecté à:"), cbAssign);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Operateur o = new Operateur(
                    "OP" + tfNum.getText().trim(),
                    tfNom.getText().trim(),
                    tfPren.getText().trim(),
                    FXCollections.observableArrayList()
                );
                o.setAssignedEquip(cbAssign.getValue());
                operateurs.add(o);
                return o;
            }
            return null;
        });
        d.showAndWait();
    }

    private void showAddEventDialog() {
        if (maintenanceFile == null) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Créer ou sélectionner maintenance");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
            maintenanceFile = fc.showSaveDialog(null);
            if (maintenanceFile == null) return;
        }
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Ajouter Événement");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("ddMMyyyy");
        DateTimeFormatter tfm = DateTimeFormatter.ofPattern("HH:mm");
        DatePicker dp = new DatePicker(LocalDate.now());
        TextField t = new TextField(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(tfm));
        ComboBox<Equipement> cbE = new ComboBox<>(equipements); cbE.getSelectionModel().selectFirst();
        ComboBox<String> cbT = new ComboBox<>(FXCollections.observableArrayList("A","D")); cbT.getSelectionModel().selectFirst();
        ComboBox<String> cbC = new ComboBox<>(FXCollections.observableArrayList("panne","maintenance","accident","ok")); cbC.getSelectionModel().selectFirst();
        ComboBox<Operateur> cbO = new ComboBox<>(operateurs.filtered(o -> !o.isBusy())); cbO.getSelectionModel().selectFirst();
        g.addRow(0, new Label("Date"), dp);
        g.addRow(1, new Label("Heure"), t);
        g.addRow(2, new Label("Équipement"), cbE);
        g.addRow(3, new Label("Type A/D"), cbT);
        g.addRow(4, new Label("Opérateur"), cbO);
        g.addRow(5, new Label("Cause"), cbC);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                // Enregistrement format espace pour compatibilité avec suiviMaintenance.txt
                String line = dp.getValue().format(df) + " "
                             + t.getText() + " "
                             + cbE.getValue().getRef() + " "
                             + cbT.getValue() + " "
                             + cbO.getValue().getCode() + " "
                             + cbC.getValue();
                try (FileWriter fw = new FileWriter(maintenanceFile, true)) {
                    fw.write(line + System.lineSeparator());
                } catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Échec écriture : " + ex.getMessage()).showAndWait();
                }
                loadEventsAndFiability();
            }
            return null;
        });
        d.showAndWait();
    }

    // === DIALOGS DE MODIFICATION ===
    private void showEditMachineDialog(Machine m) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Machine " + m.getRef());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef = new TextField(m.getRef());
        TextField tfDes = new TextField(m.getDesignation());
        TextField tfType = new TextField(m.getType());
        TextField tfCost = new TextField(Float.toString(m.getCostHourly()));
        TextField tfX = new TextField(Float.toString(m.getX()));
        TextField tfY = new TextField(Float.toString(m.getY()));
        CheckBox cbAuto = new CheckBox("Automatique"); cbAuto.setSelected(m.isAutomatic());
        g.addRow(0, new Label("Réf:"), tfRef);
        g.addRow(1, new Label("Désignation:"), tfDes);
        g.addRow(2, new Label("Type:"), tfType);
        g.addRow(3, new Label("Coût h:"), tfCost);
        g.addRow(4, new Label("X:"), tfX);
        g.addRow(5, new Label("Y:"), tfY);
        g.addRow(6, cbAuto);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    m.setRef(tfRef.getText().trim());
                    m.setDesignation(tfDes.getText().trim());
                    m.setType(tfType.getText().trim());
                    m.setCostHourly(Float.parseFloat(tfCost.getText().trim()));
                    m.setX(Float.parseFloat(tfX.getText().trim()));
                    m.setY(Float.parseFloat(tfY.getText().trim()));
                    m.setAutomatic(cbAuto.isSelected());
                    machineListView.refresh();
                    drawMachines(atelierCanvas.getGraphicsContext2D());
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Entrée invalide").showAndWait();
                }
            }
            return null;
        });
        d.showAndWait();
    }

    private void showEditPosteDialog(Poste p) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Poste " + p.getRef());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef = new TextField(p.getRef());
        ListView<Machine> lv = new ListView<>(machines);
        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        p.getMachines().forEach(lv.getSelectionModel()::select);
        g.addRow(0, new Label("Réf Poste:"), tfRef);
        g.addRow(1, new Label("Machines:"), lv);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                p.setRef(tfRef.getText().trim());
                p.setDesignation(tfRef.getText().trim());
                p.setMachines(lv.getSelectionModel().getSelectedItems());
                posteListView.refresh();
            }
            return null;
        });
        d.showAndWait();
    }

    private void showEditOperateurDialog(Operateur o) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Opérateur " + o.getCode());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfNom = new TextField(o.getNom());
        TextField tfPren = new TextField(o.getPrenom());
        ComboBox<Equipement> cbAssign = new ComboBox<>(equipements);
        cbAssign.getSelectionModel().select(o.getAssignedEquip());
        g.addRow(0, new Label("Nom:"), tfNom);
        g.addRow(1, new Label("Prénom:"), tfPren);
        g.addRow(2, new Label("Affecté à:"), cbAssign);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                o.setNom(tfNom.getText().trim());
                o.setPrenom(tfPren.getText().trim());
                o.setAssignedEquip(cbAssign.getValue());
                operListView.refresh();
            }
            return null;
        });
        d.showAndWait();
    }

    private void showEditGammeDialog(Gamme g) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Gamme " + g.getRef());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField tfRef = new TextField(g.getRef());
        grid.addRow(0, new Label("Réf Gamme:"), tfRef);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                g.modifierGamme(tfRef.getText().trim());
                gammeListView.refresh();
            }
            return null;
        });
        d.showAndWait();
    }

    private void showEditOperationDialog(Operation op) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Opération " + op.getRef());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef = new TextField(op.getRef());
        TextField tfDes = new TextField(op.getDesignation());
        TextField tfDur = new TextField(Float.toString(op.getDuree()));
        ComboBox<Machine> cbMach = new ComboBox<>(machines);
        cbMach.getSelectionModel().select((Machine) op.getEquipement());
        g.addRow(0, new Label("Réf Op:"), tfRef);
        g.addRow(1, new Label("Désignation:"), tfDes);
        g.addRow(2, new Label("Durée (h):"), tfDur);
        g.addRow(3, new Label("Machine:"), cbMach);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                op.setRef(tfRef.getText().trim());
                op.setDesignation(tfDes.getText().trim());
                try { op.setDuree(Float.parseFloat(tfDur.getText().trim())); } catch (Exception ignored) {}
                op.setEquipement(cbMach.getValue());
                operationListView.refresh();
            }
            return null;
        });
        d.showAndWait();
    }

    // === Maintenance & fiabilité ===

    private void selectMaintenanceFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Charger maintenance");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
        File chosen = fc.showOpenDialog(null);
        if (chosen != null) {
            maintenanceFile = chosen;
            loadEventsAndFiability();
        }
    }

    private void loadEventsAndFiability() {
        eventsListView.getItems().clear();
        maintenanceListView.getItems().clear();
        if (maintenanceFile == null) return;

        class Ev { LocalDateTime dt; boolean isUp; Ev(LocalDateTime dt, boolean isUp){this.dt=dt;this.isUp=isUp;} }
        Map<String,Map<LocalDate,List<Ev>>> byMachine = new HashMap<>();
        Set<LocalDate> dates = new HashSet<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("ddMMyyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(maintenanceFile))) {
            String l;
            while ((l = br.readLine()) != null) {
                if (l.isBlank()) continue;
                // Support des fichiers espace-delimités
                // nouveau : supporte à la fois ";" et les espaces
                String[] p = l.trim().split("[;\\s]+");

                if (p.length < 4) continue;
                LocalDate d = LocalDate.parse(p[0], df);
                LocalTime t = LocalTime.parse(p[1], tf);
                String ref = p[2];
                boolean isUp = p[3].equals("D");
                dates.add(d);
                byMachine
                  .computeIfAbsent(ref, k->new HashMap<>())
                  .computeIfAbsent(d, k->new ArrayList<>())
                  .add(new Ev(LocalDateTime.of(d, t), isUp));
                eventsListView.getItems().add(l);
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur lecture : " + ex.getMessage()).showAndWait();
            return;
        }

        if (dates.isEmpty()) {
            machines.forEach(m ->
                maintenanceListView.getItems().add(m.getRef() + " : 100.00%")
            );
            return;
        }

        LocalTime START = LocalTime.of(6, 0), END = LocalTime.of(20, 0);
        long secDay = ChronoUnit.SECONDS.between(START, END);
        long total = dates.size() * secDay;
        Map<String,Float> rates = new HashMap<>();

        for (var ent : byMachine.entrySet()) {
            String ref = ent.getKey();
            long downSec = 0;
            for (LocalDate d : dates) {
                LocalDateTime S = LocalDateTime.of(d, START);
                LocalDateTime E = LocalDateTime.of(d, END);
                List<Ev> evs = ent.getValue().getOrDefault(d, List.of());
                evs.sort(Comparator.comparing(e -> e.dt));
                boolean down = false; LocalDateTime at = null;
                for (Ev ev : evs) {
                    if (ev.dt.isBefore(S) || ev.dt.isAfter(E)) continue;
                    if (!ev.isUp && !down) { down = true; at = ev.dt; }
                    else if (ev.isUp && down) {
                        downSec += ChronoUnit.SECONDS.between(at, ev.dt);
                        down = false;
                    }
                }
                if (down && at != null) {
                    downSec += ChronoUnit.SECONDS.between(at, E);
                }
            }
            long upSec = total - downSec;
            float r = upSec <= 0 ? 0f : (float) upSec / total;
            rates.put(ref, Math.max(0f, Math.min(1f, r)));
        }

        Set<String> seen = new HashSet<>();
        for (Machine m : machines) {
            float r = rates.getOrDefault(m.getRef(), 1f);
            maintenanceListView.getItems().add(
                m.getRef() + " : " + String.format("%.2f%%", r * 100)
            );
            seen.add(m.getRef());
        }
        for (String ref : rates.keySet()) {
            if (seen.add(ref)) {
                float r = rates.get(ref);
                maintenanceListView.getItems().add(
                    ref + " : " + String.format("%.2f%%", r * 100)
                );
            }
        }
    }

    // === Exécution Gamme & dessin & utilitaires ===

    private void updateGammeStats(Gamme g) {
        lblGammeCost    .setText(String.format("Coût: %.2f", g.coutGamme()));
        lblGammeDuration.setText(String.format("Durée: %.2f", g.dureeGamme()));
    }

    private void startGamme(Gamme g) {
        for (Operation op : g.getOperations()) {
            if (op.getEquipement() instanceof Machine) {
                Machine m = (Machine) op.getEquipement();
                m.setAvailable(false);
                if (!m.isAutomatic()) {
                    operateurs.stream()
                      .filter(o -> {
                          Equipement a = o.getAssignedEquip();
                          return a == m || (a instanceof Poste && ((Poste) a).getMachines().contains(m));
                      })
                      .findFirst()
                      .ifPresent(o -> o.setBusy(true));
                }
            }
        }
        machineListView.refresh();
        operListView.refresh();
        drawMachines(atelierCanvas.getGraphicsContext2D());
    }

    private void drawMachines(GraphicsContext gc) {
        gc.clearRect(0, 0, atelierCanvas.getWidth(), atelierCanvas.getHeight());
        for (Equipement eq : equipements) {
            if (eq instanceof Machine) {
                Machine m = (Machine) eq;
                gc.fillOval(m.getX(), m.getY(), 20, 20);
                gc.fillText(m.getRef(), m.getX(), m.getY() - 5);
            } else if (eq instanceof Poste) {
                Poste p = (Poste) eq;
                gc.strokeRect(10, 10, 100, 50);
                gc.fillText(p.getRef(), 15, 25);
            }
        }
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    // === CellFactories ===

    private class MachineCell extends ListCell<Machine> {
        @Override protected void updateItem(Machine m, boolean empty) {
            super.updateItem(m, empty);
            if (empty || m == null) { setText(null); setContextMenu(null); return; }
            setText(m.getRef() + " – " + m.getDesignation()
                + (m.isAutomatic() ? " (Auto)" : "")
                + (m.isAvailable() ? " (libre)" : " (occupée)"));
            MenuItem edit = new MenuItem("Modifier");
            edit.setOnAction(e -> showEditMachineDialog(m));
            MenuItem del = new MenuItem("Supprimer");
            del.setOnAction(e -> {
                if (confirm("Supprimer machine " + m.getRef() + " ?")) {
                    machines.remove(m);
                    equipements.remove(m);
                    machineListView.refresh();
                    drawMachines(atelierCanvas.getGraphicsContext2D());
                }
            });
            setContextMenu(new ContextMenu(edit, del));
        }
    }

    private class PosteCell extends ListCell<Poste> {
        @Override protected void updateItem(Poste p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) { setText(null); setContextMenu(null); return; }
            setText(p.getRef());
            MenuItem edit = new MenuItem("Modifier");
            edit.setOnAction(e -> showEditPosteDialog(p));
            MenuItem del = new MenuItem("Supprimer");
            del.setOnAction(e -> {
                if (confirm("Supprimer poste " + p.getRef() + " ?")) {
                    postes.remove(p);
                    equipements.remove(p);
                    posteListView.refresh();
                }
            });
            setContextMenu(new ContextMenu(edit, del));
        }
    }

    private class OperateurCell extends ListCell<Operateur> {
        @Override protected void updateItem(Operateur o, boolean empty) {
            super.updateItem(o, empty);
            if (empty || o == null) { setText(null); setContextMenu(null); return; }
            setText(o.getCode() + " – " + o.getNom() + " " + o.getPrenom()
                + (o.isBusy() ? " (occupé)" : " (libre)"));
            MenuItem edit = new MenuItem("Modifier");
            edit.setOnAction(e -> showEditOperateurDialog(o));
            MenuItem del = new MenuItem("Supprimer");
            del.setOnAction(e -> {
                if (confirm("Supprimer opérateur " + o.getCode() + " ?")) {
                    operateurs.remove(o);
                    operListView.refresh();
                }
            });
            setContextMenu(new ContextMenu(edit, del));
        }
    }

    private class GammeCell extends ListCell<Gamme> {
        @Override protected void updateItem(Gamme g, boolean empty) {
            super.updateItem(g, empty);
            if (empty || g == null) { setText(null); setContextMenu(null); return; }
            setText(g.getRef());
            MenuItem edit = new MenuItem("Modifier");
            edit.setOnAction(e -> showEditGammeDialog(g));
            MenuItem del = new MenuItem("Supprimer");
            del.setOnAction(e -> {
                if (confirm("Supprimer gamme " + g.getRef() + " ?")) {
                    gammes.remove(g);
                    operationListView.getItems().clear();
                }
            });
            setContextMenu(new ContextMenu(edit, del));
        }
    }

    private class OperationCell extends ListCell<Operation> {
        @Override protected void updateItem(Operation op, boolean empty) {
            super.updateItem(op, empty);
            if (empty || op == null) { setText(null); setContextMenu(null); return; }
            setText(op.getRef() + " – " + op.getDesignation());
            MenuItem edit = new MenuItem("Modifier");
            edit.setOnAction(e -> showEditOperationDialog(op));
            MenuItem del = new MenuItem("Supprimer");
            del.setOnAction(e -> {
                Gamme g = gammeListView.getSelectionModel().getSelectedItem();
                if (g != null && confirm("Supprimer opération " + op.getRef() + " ?")) {
                    g.getOperations().remove(op);
                    operationListView.getItems().remove(op);
                    updateGammeStats(g);
                }
            });
            setContextMenu(new ContextMenu(edit, del));
        }
    }
}
