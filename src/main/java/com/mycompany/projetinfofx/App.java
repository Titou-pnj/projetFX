// App.java
package com.mycompany.projetinfofx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;

public class App extends Application {
    // Données
    private final ObservableList<Equipement> equipements = FXCollections.observableArrayList();
    private final ObservableList<Machine> machines    = FXCollections.observableArrayList();
    private final ObservableList<Poste>   postes      = FXCollections.observableArrayList();
    private final ObservableList<Operateur> operateurs = FXCollections.observableArrayList();
    private final ObservableList<Gamme>   gammes      = FXCollections.observableArrayList();

    // UI
    private ListView<Machine> machineListView;
    private ListView<Poste>   posteListView;
    private ListView<Operateur> operListView;
    private ListView<Gamme>   gammeListView;
    private ListView<Operation> operationListView;
    private ListView<String>  eventsListView;
    private ListView<String>  maintenanceListView;
    private Canvas            atelierCanvas;
    private File              maintenanceFile;
    private Label             lblGammeCost;
    private Label             lblGammeDuration;
    private Timeline          currentTimeline;
    private long              remainingMillis;

    private static final double MACHINE_RADIUS = 10;

    /** Point d'entrée de l'application. */
    public static void main(String[] args) {
        launch(args);
    }

    /** Démarre l'interface et initialise toutes les composantes.
     * @param stage */
    @Override
    public void start(Stage stage) {
        // Top bar
        Label title = new Label("Atelier de Fabrication");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold;");
        Button btnAddMachine = new Button("Ajouter Machine");
        Button btnAddPoste   = new Button("Ajouter Poste");
        Button btnAddOper    = new Button("Ajouter Opérateur");
        Button btnLoadEvents = new Button("Charger Événements");
        Button btnAddEvent   = new Button("Ajouter Événement");

        btnAddMachine.setOnAction(e -> afficherDialogueAjoutMachine());
        btnAddPoste  .setOnAction(e -> afficherDialogueAjoutPoste());
        btnAddOper   .setOnAction(e -> afficherDialogueAjoutOperateur());
        btnLoadEvents.setOnAction(e -> sélectionnerFichierMaintenance());
        btnAddEvent  .setOnAction(e -> afficherDialogueAjoutÉvénement());

        HBox topBar = new HBox(10, title, btnAddMachine, btnAddPoste, btnAddOper, btnLoadEvents, btnAddEvent);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        // Tabs
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Équipements", créerPanneauEquipements()),
            new Tab("Opérateurs",  créerPanneauOpérateurs()),
            new Tab("Gammes",      créerPanneauGammes()),
            new Tab("Maintenance", créerPanneauMaintenance()),
            new Tab("Atelier",     créerPanneauAtelier())
        );

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 1000, 750));
        stage.setTitle("Gestion Atelier JavaFX");
        stage.show();

        chargerÉvénementsEtFiabilité();
    }

    /** Crée le panneau pour gérer les machines et postes. */
    private VBox créerPanneauEquipements() {
        machineListView = new ListView<>(machines);
        machineListView.setCellFactory(lv -> new MachineCell());
        posteListView   = new ListView<>(postes);
        posteListView.setCellFactory(lv -> new PosteCell());
        VBox box = new VBox(10, new Label("Machines"), machineListView, new Label("Postes"), posteListView);
        box.setPadding(new Insets(10));
        return box;
    }

    /** Crée le panneau pour gérer les opérateurs. */
    private VBox créerPanneauOpérateurs() {
        operListView = new ListView<>(operateurs);
        operListView.setCellFactory(lv -> new OperateurCell());
        VBox box = new VBox(10, new Label("Opérateurs"), operListView);
        box.setPadding(new Insets(10));
        return box;
    }

    /** Crée le panneau pour gérer les gammes et leurs opérations. */
    private BorderPane créerPanneauGammes() {
        gammeListView     = new ListView<>(gammes);
        gammeListView.setCellFactory(lv -> new GammeCell());
        operationListView = new ListView<>();
        operationListView.setCellFactory(lv -> new OperationCell());

        lblGammeCost     = new Label("Coût: 0");
        lblGammeDuration = new Label("Durée: 0");

        TextField tfOpRef = new TextField(); tfOpRef.setPromptText("Réf Op");
        TextField tfOpDes = new TextField(); tfOpDes.setPromptText("Désignation");
        TextField tfOpDur = new TextField(); tfOpDur.setPromptText("Durée (h)");
        ComboBox<Machine> cbMach = new ComboBox<>(machines);
        cbMach.getSelectionModel().selectFirst();
        Button btnAddOp = new Button("Ajouter Opération");
        btnAddOp.disableProperty().bind(
            gammeListView.getSelectionModel().selectedItemProperty().isNull()
        );
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
                    g.ajouterOpération(op);
                    operationListView.getItems().add(op);
                    mettreÀJourStatsGamme(g);
                    tfOpRef.clear(); tfOpDes.clear(); tfOpDur.clear();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Durée invalide").showAndWait();
                }
            }
        });

        Button btnStart = new Button("Démarrer");
        VBox activeTimersBox = new VBox(5);
        btnStart.disableProperty().bind(
            gammeListView.getSelectionModel().selectedItemProperty().isNull()
        );
        btnStart.setOnAction(e -> {
            Gamme g = gammeListView.getSelectionModel().getSelectedItem();
            if (g != null) {
                HBox timerRow = new HBox(10);
                Label timerLabel = new Label("00:00:00");
                Button btnPause = new Button("Pause");
                Button btnStop  = new Button("Stop");
                timerRow.getChildren().addAll(new Label(g.obtenirRéférence()+":"), timerLabel, btnPause, btnStop);
                activeTimersBox.getChildren().add(timerRow);

                démarrerGammeAvecChrono(g, timerLabel, btnPause, btnStop);
            }
        });

        VBox center = new VBox(10,
            new Label("Opérations"), operationListView,
            new HBox(10, tfOpRef, tfOpDes, tfOpDur, cbMach, btnAddOp),
            new HBox(20, lblGammeCost, lblGammeDuration),
            btnStart,
            new Label("Chronos actifs:"), activeTimersBox
        );
        center.setPadding(new Insets(10));

        TextField tfRefG = new TextField(); tfRefG.setPromptText("Réf Gamme");
        Button btnNewG = new Button("Créer Gamme");
        btnNewG.setOnAction(e -> {
            String ref = tfRefG.getText().trim();
            if (!ref.isEmpty()) { gammes.add(new Gamme(ref)); tfRefG.clear(); }
        });
        VBox left = new VBox(10, new Label("Gammes"), tfRefG, btnNewG, gammeListView);
        left.setPadding(new Insets(10));
        left.setPrefWidth(200);

        gammeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldv, newv) -> {
            operationListView.getItems().clear();
            if (newv != null) {
                operationListView.getItems().addAll(newv.obtenirOpérations());
                mettreÀJourStatsGamme(newv);
            }
        });

        return new BorderPane(center, null, null, null, left);
    }

    /** Démarre une gamme en affichant et gérant un chronomètre. */
    private void démarrerGammeAvecChrono(Gamme g, Label timerLabel, Button btnPause, Button btnStop) {
        démarrerGamme(g);
        remainingMillis = (long)(g.calculerDurée() * 3600_000);
        currentTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            remainingMillis -= 1000;
            long secs = remainingMillis / 1000;
            long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
            if (remainingMillis <= 0) {
                currentTimeline.stop();
                terminerGamme(g);
                timerLabel.setText("Terminé");
                btnPause.setDisable(true);
                btnStop .setDisable(true);
            }
        }));
        currentTimeline.setCycleCount(Timeline.INDEFINITE);
        currentTimeline.play();

        btnPause.setOnAction(e -> {
            if (currentTimeline.getStatus() == Animation.Status.RUNNING) {
                currentTimeline.pause();
                btnPause.setText("Reprendre");
            } else {
                currentTimeline.play();
                btnPause.setText("Pause");
            }
        });

        btnStop.setOnAction(e -> {
            currentTimeline.stop();
            terminerGamme(g);
            timerLabel.setText("Arrêté");
            btnPause.setDisable(true);
            btnStop .setDisable(true);
        });
    }

    /** Crée le panneau pour le journal d'événements et la fiabilité. */
    private VBox créerPanneauMaintenance() {
        eventsListView      = new ListView<>();
        maintenanceListView = new ListView<>();
        VBox box = new VBox(10,
            new Label("Journal des événements"), eventsListView,
            new Label("Fiabilité"), maintenanceListView
        );
        box.setPadding(new Insets(10));
        return box;
    }

    /** Crée le panneau pour afficher l'atelier avec ses machines. */
    private StackPane créerPanneauAtelier() {
        atelierCanvas = new Canvas(900, 600);
        dessinerMachines(atelierCanvas.getGraphicsContext2D());
        StackPane sp = new StackPane(atelierCanvas);
        sp.setPadding(new Insets(10));
        return sp;
    }

    /** Dessine les machines et les postes sur le canvas. */
    private void dessinerMachines(GraphicsContext gc) {
        gc.clearRect(0, 0, atelierCanvas.getWidth(), atelierCanvas.getHeight());
        for (Equipement eq : equipements) {
            if (eq instanceof Machine) {
                Machine m = (Machine) eq;
                gc.setFill(Color.GRAY);
                gc.fillOval(m.obtenirX(), m.obtenirY(), MACHINE_RADIUS*2, MACHINE_RADIUS*2);
                gc.setFill(Color.BLACK);
                gc.fillText(m.obtenirRéférence(), m.obtenirX(), m.obtenirY() - 5);
            }
        }
        for (Poste p : postes) {
            List<Machine> group = new ArrayList<>(p.obtenirMachines());
            if (group.isEmpty()) continue;
            double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE;
            double maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
            for (Machine m : group) {
                minX = Math.min(minX, m.obtenirX());
                minY = Math.min(minY, m.obtenirY());
                maxX = Math.max(maxX, m.obtenirX());
                maxY = Math.max(maxY, m.obtenirY());
            }
            double pad = 5;
            gc.setStroke(Color.BLUE); gc.setLineWidth(2);
            gc.strokeRect(minX-pad, minY-pad,
                (maxX-minX)+MACHINE_RADIUS*2+pad*2,
                (maxY-minY)+MACHINE_RADIUS*2+pad*2);
            gc.setFill(Color.BLUE);
            gc.fillText(p.obtenirRéférence(), minX, minY-pad-2);
        }
    }

    /** Affiche le dialogue pour ajouter une nouvelle machine. */
    private void afficherDialogueAjoutMachine() {
        Dialog<Machine> d = new Dialog<>();
        d.setTitle("Ajouter Machine");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        TextField tfRef  = new TextField();
        TextField tfDes  = new TextField();
        TextField tfType = new TextField();
        TextField tfCost = new TextField();
        TextField tfX    = new TextField();
        TextField tfY    = new TextField();
        CheckBox cbAuto  = new CheckBox("Automatique");
        g.addRow(0, new Label("Réf:"),         tfRef);
        g.addRow(1, new Label("Désignation:"), tfDes);
        g.addRow(2, new Label("Type:"),        tfType);
        g.addRow(3, new Label("Coût h:"),      tfCost);
        g.addRow(4, new Label("X:"),           tfX);
        g.addRow(5, new Label("Y:"),           tfY);
        g.addRow(6, cbAuto);
        d.getDialogPane().setContent(g);
        Button okBtn = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        BooleanBinding coordValid = Bindings.createBooleanBinding(() -> {
            try {
                float x = Float.parseFloat(tfX.getText().trim());
                float y = Float.parseFloat(tfY.getText().trim());
                double maxX = atelierCanvas.getWidth()  - MACHINE_RADIUS * 2;
                double maxY = atelierCanvas.getHeight() - MACHINE_RADIUS * 2;
                return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
            } catch (Exception e) {
                return false;
            }
        }, tfX.textProperty(), tfY.textProperty());
        okBtn.disableProperty().bind(coordValid.not());
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
                    m.définirAutomatique(cbAuto.isSelected());
                    machines.add(m);
                    equipements.add(m);
                    dessinerMachines(atelierCanvas.getGraphicsContext2D());
                    return m;
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Valeur invalide").showAndWait();
                }
            }
            return null;
        });
        d.showAndWait();
    }

    /** Affiche le dialogue pour modifier une machine existante. */
    private void afficherDialogueModificationMachine(Machine m) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Machine " + m.obtenirRéférence());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        TextField tfRef  = new TextField(m.obtenirRéférence());
        TextField tfDes  = new TextField(m.obtenirDésignation());
        TextField tfType = new TextField(m.obtenirType());
        TextField tfCost = new TextField(Float.toString(m.obtenirCoûtHoraire()));
        TextField tfX    = new TextField(Float.toString(m.obtenirX()));
        TextField tfY    = new TextField(Float.toString(m.obtenirY()));
        CheckBox cbAuto  = new CheckBox("Automatique");
        cbAuto.setSelected(m.estAutomatique());
        g.addRow(0, new Label("Réf:"),         tfRef);
        g.addRow(1, new Label("Désignation:"), tfDes);
        g.addRow(2, new Label("Type:"),        tfType);
        g.addRow(3, new Label("Coût h:"),      tfCost);
        g.addRow(4, new Label("X:"),           tfX);
        g.addRow(5, new Label("Y:"),           tfY);
        g.addRow(6, cbAuto);
        d.getDialogPane().setContent(g);
        Button okBtn = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        BooleanBinding coordValidMod = Bindings.createBooleanBinding(() -> {
            try {
                float x = Float.parseFloat(tfX.getText().trim());
                float y = Float.parseFloat(tfY.getText().trim());
                double maxX = atelierCanvas.getWidth()  - MACHINE_RADIUS * 2;
                double maxY = atelierCanvas.getHeight() - MACHINE_RADIUS * 2;
                return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
            } catch (Exception e) {
                return false;
            }
        }, tfX.textProperty(), tfY.textProperty());
        okBtn.disableProperty().bind(coordValidMod.not());
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                m.définirRéférence(tfRef.getText().trim());
                m.définirDésignation(tfDes.getText().trim());
                m.définirType(tfType.getText().trim());
                m.définirCoûtHoraire(Float.parseFloat(tfCost.getText().trim()));
                m.définirX(Float.parseFloat(tfX.getText().trim()));
                m.définirY(Float.parseFloat(tfY.getText().trim()));
                m.définirAutomatique(cbAuto.isSelected());
                machineListView.refresh();
                dessinerMachines(atelierCanvas.getGraphicsContext2D());
            }
            return null;
        });
        d.showAndWait();
    }

    /** Affiche le dialogue pour ajouter un nouveau poste. */
    private void afficherDialogueAjoutPoste() {
        Dialog<Poste> d = new Dialog<>();
        d.setTitle("Ajouter Poste");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        TextField tfRef = new TextField();
        tfRef.setPromptText("Réf Poste");
        ListView<Machine> lv = new ListView<>(machines);
        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        g.addRow(0, new Label("Réf Poste:"), tfRef);
        g.addRow(1, new Label("Machines:"), lv);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String ref = tfRef.getText().trim();
                List<Machine> sélection = new ArrayList<>(lv.getSelectionModel().getSelectedItems());
                Poste p = new Poste(ref, ref, sélection);
                postes.add(p);
                equipements.add(p);
                dessinerMachines(atelierCanvas.getGraphicsContext2D());
                return p;
            }
            return null;
        });
        d.showAndWait();
    }

    /** Affiche le dialogue pour modifier un poste existant. */
    private void afficherDialogueModificationPoste(Poste p) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Modifier Poste " + p.obtenirRéférence());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        TextField tfRef = new TextField(p.obtenirRéférence());
        ListView<Machine> lv = new ListView<>(machines);
        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lv.getSelectionModel().clearSelection();
        for (Machine m : p.obtenirMachines()) {
            lv.getSelectionModel().select(m);
        }
        g.addRow(0, new Label("Réf Poste:"), tfRef);
        g.addRow(1, new Label("Machines:"), lv);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String nouvelleRef = tfRef.getText().trim();
                List<Machine> sélection = new ArrayList<>(lv.getSelectionModel().getSelectedItems());
                p.définirRéférence(nouvelleRef);
                p.définirDésignation(nouvelleRef);
                p.définirMachines(sélection);
                posteListView.refresh();
                dessinerMachines(atelierCanvas.getGraphicsContext2D());
            }
            return null;
        });
        d.showAndWait();
    }

    /** Affiche le dialogue pour ajouter un nouvel opérateur. */
    private void afficherDialogueAjoutOperateur() {
        Dialog<Operateur> d = new Dialog<>();
        d.setTitle("Ajouter Opérateur");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        TextField tfNum  = new TextField();
        TextField tfNom  = new TextField();
        TextField tfPren = new TextField();
        ComboBox<Equipement> cbAssign = new ComboBox<>();
        cbAssign.getItems().add(null);
        cbAssign.getItems().addAll(equipements);
        cbAssign.getSelectionModel().selectFirst();
        g.addRow(0, new Label("Num OP:"), tfNum);
        g.addRow(1, new Label("Nom:"),      tfNom);
        g.addRow(2, new Label("Prénom:"),   tfPren);
        g.addRow(3, new Label("Affecté à:"),cbAssign);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Operateur o = new Operateur(
                    "OP" + tfNum.getText().trim(),
                    tfNom.getText().trim(),
                    tfPren.getText().trim(),
                    new ArrayList<>()
                );
                Equipement sel = cbAssign.getValue();
                if (sel != null) {
                    o.définirÉquipementAffecté(sel);
                }
                operateurs.add(o);
                return o;
            }
            return null;
        });
        d.showAndWait();
    }

    /** Affiche le dialogue pour ajouter une opération à une gamme. */
    private void afficherDialogueAjoutOpération() {
        Gamme g = gammeListView.getSelectionModel().getSelectedItem();
        if (g == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une gamme d'abord.").showAndWait();
            return;
        }
        Dialog<Operation> d = new Dialog<>();
        d.setTitle("Ajouter Opération à " + g.obtenirRéférence());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField tfRef   = new TextField(); tfRef.setPromptText("Réf Op");
        TextField tfDes   = new TextField(); tfDes.setPromptText("Désignation");
        TextField tfDur   = new TextField(); tfDur.setPromptText("Durée (h)");
        ComboBox<Machine> cbMach = new ComboBox<>(machines);
        cbMach.getSelectionModel().selectFirst();
        grid.addRow(0, new Label("Réf Op:"), tfRef);
        grid.addRow(1, new Label("Désignation:"), tfDes);
        grid.addRow(2, new Label("Durée (h):"), tfDur);
        grid.addRow(3, new Label("Machine:"), cbMach);
        d.getDialogPane().setContent(grid);
        Button okBtn = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        BooleanBinding valid = Bindings.createBooleanBinding(() -> {
            try {
                return !tfRef.getText().trim().isEmpty()
                    && !tfDes.getText().trim().isEmpty()
                    && Float.parseFloat(tfDur.getText().trim()) > 0;
            } catch (Exception e) {
                return false;
            }
        }, tfRef.textProperty(), tfDes.textProperty(), tfDur.textProperty());
        okBtn.disableProperty().bind(valid.not());
        d.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                float dur = Float.parseFloat(tfDur.getText().trim());
                return new Operation(
                    tfRef.getText().trim(),
                    tfDes.getText().trim(),
                    cbMach.getValue(),
                    dur
                );
            }
            return null;
        });
        Optional<Operation> result = d.showAndWait();
        result.ifPresent(op -> {
            g.ajouterOpération(op);
            operationListView.getItems().add(op);
            mettreÀJourStatsGamme(g);
        });
    }

    /** Ouvre le sélecteur pour choisir le fichier des événements de maintenance. */
    private void sélectionnerFichierMaintenance() {
        FileChooser fc=new FileChooser();
        fc.setTitle("Charger exemple d'événements");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte","*.txt"));
        File choisi=fc.showOpenDialog(null);
        if(choisi!=null){ maintenanceFile=choisi; chargerÉvénementsEtFiabilité(); }
    }

    /** Affiche le dialogue pour ajouter un événement de maintenance. */
    private void afficherDialogueAjoutÉvénement() {
        if(maintenanceFile==null){ new Alert(Alert.AlertType.WARNING,"Veuillez d'abord charger ou créer un fichier d'événements.").showAndWait(); return; }
        Dialog<Void> d=new Dialog<>(); d.setTitle("Ajouter Événement");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g=new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        DateTimeFormatter df=DateTimeFormatter.ofPattern("ddMMyyyy");
        DateTimeFormatter tfm=DateTimeFormatter.ofPattern("HH:mm");
        DatePicker dp=new DatePicker(LocalDate.now());
        TextField t=new TextField(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(tfm));
        ComboBox<Equipement> cbE=new ComboBox<>(equipements); cbE.getSelectionModel().selectFirst();
        ComboBox<String> cbT=new ComboBox<>(FXCollections.observableArrayList("A","D")); cbT.getSelectionModel().selectFirst();
        ComboBox<Operateur> cbO=new ComboBox<>(operateurs.filtered(o->!o.estOccupé())); cbO.getSelectionModel().selectFirst();
        TextField tfCause=new TextField(); tfCause.setPromptText("Cause");
        g.addRow(0,new Label("Date"),dp);
        g.addRow(1,new Label("Heure"),t);
        g.addRow(2,new Label("Équipement"),cbE);
        g.addRow(3,new Label("Type (A/D)"),cbT);
        g.addRow(4,new Label("Opérateur"),cbO);
        g.addRow(5,new Label("Cause"),tfCause);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt->{
            if(bt==ButtonType.OK){
                String line=dp.getValue().format(df)+" "+t.getText()+" "+cbE.getValue().obtenirRéférence()+" "+cbT.getValue()+" "+cbO.getValue().obtenirCode()+" "+tfCause.getText().trim();
                try(FileWriter fw=new FileWriter(maintenanceFile,true)){ fw.write(line+System.lineSeparator()); } catch(IOException ex){ new Alert(Alert.AlertType.ERROR,"Échec écriture : "+ex.getMessage()).showAndWait(); }
                chargerÉvénementsEtFiabilité();
            }
            return null;
        }); d.showAndWait();
    }

    /** Affiche le dialogue pour modifier un opérateur existant. */
    private void afficherDialogueModificationOpérateur(Operateur o) {
        Dialog<Void> d=new Dialog<>(); d.setTitle("Modifier Opérateur "+o.obtenirCode());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        GridPane g=new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfNom=new TextField(o.obtenirNom()), tfPren=new TextField(o.obtenirPrénom());
        ComboBox<Equipement> cbAssign=new ComboBox<>(equipements); cbAssign.getSelectionModel().select(o.obtenirÉquipementAffecté());
        g.addRow(0,new Label("Nom:"),tfNom);
        g.addRow(1,new Label("Prénom:"),tfPren);
        g.addRow(2,new Label("Affecté à:"),cbAssign);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt->{ if(bt==ButtonType.OK){ o.définirNom(tfNom.getText().trim()); o.définirPrénom(tfPren.getText().trim()); o.définirÉquipementAffecté(cbAssign.getValue()); operListView.refresh();} return null; });
        d.showAndWait();
    }

    /** Affiche le dialogue pour modifier une gamme existante. */
    private void afficherDialogueModificationGamme(Gamme g) {
        Dialog<Void> d=new Dialog<>(); d.setTitle("Modifier Gamme "+g.obtenirRéférence());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        GridPane grid=new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField tfRef=new TextField(g.obtenirRéférence()); grid.addRow(0,new Label("Réf Gamme:"),tfRef);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(bt->{ if(bt==ButtonType.OK){ g.définirRéférence(tfRef.getText().trim()); gammeListView.refresh(); } return null; });
        d.showAndWait();
    }

    /** Affiche le dialogue pour modifier une opération existante. */
    private void afficherDialogueModificationOpération(Operation op) {
        Dialog<Void> d=new Dialog<>(); d.setTitle("Modifier Opération "+op.obtenirRéférence());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        GridPane g=new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField tfRef=new TextField(op.obtenirRéférence()), tfDes=new TextField(op.obtenirDésignation()), tfDur=new TextField(Float.toString(op.obtenirDurée()));
        ComboBox<Machine> cbMach=new ComboBox<>(machines); cbMach.getSelectionModel().select((Machine)op.obtenirÉquipement());
        g.addRow(0,new Label("Réf Op:"),tfRef);
        g.addRow(1,new Label("Désignation:"),tfDes);
        g.addRow(2,new Label("Durée (h):"),tfDur);
        g.addRow(3,new Label("Machine:"),cbMach);
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt->{ if(bt==ButtonType.OK){ op.définirRéférence(tfRef.getText().trim()); op.définirDésignation(tfDes.getText().trim());
                try{ op.définirDurée(Float.parseFloat(tfDur.getText().trim())); }catch(Exception ignored){}
                op.définirÉquipement(cbMach.getValue()); operationListView.refresh(); } return null; });
        d.showAndWait();
    }

    /** Charge les événements et calcule la fiabilité de chaque machine. */
    private void chargerÉvénementsEtFiabilité() {
        eventsListView.getItems().clear();
        maintenanceListView.getItems().clear();
        if (maintenanceFile == null) return;
        class Ev { LocalDateTime dt; boolean estFonctionnel; Ev(LocalDateTime dt, boolean estFonctionnel){this.dt=dt;this.estFonctionnel=estFonctionnel;} }
        Map<String,List<Ev>> map = new HashMap<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("ddMMyyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        try (BufferedReader br = new BufferedReader(new FileReader(maintenanceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.trim().split("[;\\s]+");
                if (p.length<4) continue;
                LocalDate d = LocalDate.parse(p[0], df);
                LocalTime t = LocalTime.parse(p[1], tf);
                String ref = p[2]; boolean up = p[3].equalsIgnoreCase("D");
                for (Machine m : machines) if (m.obtenirRéférence().equalsIgnoreCase(ref)) ref = m.obtenirRéférence();
                map.computeIfAbsent(ref, k->new ArrayList<>()).add(new Ev(LocalDateTime.of(d,t), up));
                eventsListView.getItems().add(line);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lecture: " + e.getMessage()).showAndWait();
            return;
        }
        if (map.isEmpty()) {
            machines.forEach(m->maintenanceListView.getItems().add(m.obtenirRéférence()+" : 100.00%"));
            return;
        }
        TreeSet<LocalDate> dates = new TreeSet<>();
        map.values().forEach(list->list.forEach(ev->dates.add(ev.dt.toLocalDate())));
        LocalTime start=LocalTime.of(6,0), end=LocalTime.of(20,0);
        long daySec=ChronoUnit.SECONDS.between(start,end), total=daySec*dates.size();
        BiFunction<String,List<Ev>,Float> comp = (r,evs)->{
            Map<LocalDate,List<Ev>> byDate=new HashMap<>(); evs.forEach(ev->byDate.computeIfAbsent(ev.dt.toLocalDate(),x->new ArrayList<>()).add(ev));
            long down=0;
            for(LocalDate d:dates){
                List<Ev> evsDay=byDate.getOrDefault(d,Collections.emptyList());
                evsDay.sort(Comparator.comparing(x->x.dt));
                boolean isDown=false; LocalDateTime at=null;
                LocalDateTime sdt=LocalDateTime.of(d,start), edt=LocalDateTime.of(d,end);
                for(Ev ev:evsDay){
                    if(ev.dt.isBefore(sdt)||ev.dt.isAfter(edt)) continue;
                    if(!ev.estFonctionnel && !isDown){ isDown=true; at=ev.dt; }
                    else if(ev.estFonctionnel && isDown){ down+=ChronoUnit.SECONDS.between(at,ev.dt); isDown=false; }
                }
                if(isDown && at!=null) down+=ChronoUnit.SECONDS.between(at,edt);
            }
            long up=total-down;
            return up>0?(float)up/total:0f;
        };
        Map<String,Float> rates=new HashMap<>();
        machines.forEach(m->rates.put(m.obtenirRéférence(), comp.apply(m.obtenirRéférence(), map.getOrDefault(m.obtenirRéférence(), Collections.emptyList()))));
        map.keySet().stream().filter(r->machines.stream().noneMatch(m->m.obtenirRéférence().equals(r))).forEach(r->rates.put(r, comp.apply(r,map.get(r))));
        maintenanceListView.getItems().clear();
        rates.entrySet().stream()
             .sorted((e1,e2)->Float.compare(e2.getValue(),e1.getValue()))
             .forEach(e->maintenanceListView.getItems().add(e.getKey()+" : "+String.format("%.2f%%",e.getValue()*100)));
    }

    /** Démarre une gamme en réservant les machines et affectant les opérateurs. */
    private void démarrerGamme(Gamme g) {
        for (Operation op : g.obtenirOpérations()) {
            if (op.obtenirÉquipement() instanceof Machine) {
                Machine m = (Machine) op.obtenirÉquipement();
                m.définirDisponible(false);
                if (!m.estAutomatique()) {
                    operateurs.stream()
                        .filter(o -> {
                            Equipement ae = o.obtenirÉquipementAffecté();
                            return ae == m
                              || (ae instanceof Poste && ((Poste) ae).obtenirMachines().contains(m));
                        })
                        .filter(o -> !o.estOccupé())
                        .findFirst()
                        .ifPresent(o -> o.définirOccupé(true));
                }
            }
        }
        machineListView.refresh();
        operListView.refresh();
    }

    /** Termine une gamme en libérant les machines et les opérateurs. */
    private void terminerGamme(Gamme g) {
        for (Operation op : g.obtenirOpérations()) {
            if (op.obtenirÉquipement() instanceof Machine) {
                Machine m = (Machine) op.obtenirÉquipement();
                m.définirDisponible(true);
                operateurs.stream()
                    .filter(o -> {
                        Equipement ae = o.obtenirÉquipementAffecté();
                        return ae == m
                          || (ae instanceof Poste && ((Poste) ae).obtenirMachines().contains(m));
                    })
                    .filter(o -> o.estOccupé())
                    .forEach(o -> o.définirOccupé(false));
            }
        }
        machineListView.refresh();
        operListView.refresh();
    }

    /** Met à jour le coût et la durée affichés pour une gamme. */
    private void mettreÀJourStatsGamme(Gamme g) {
        lblGammeCost   .setText(String.format("Coût: %.2f", g.calculerCoût()));
        lblGammeDuration.setText(String.format("Durée: %.2f", g.calculerDurée()));
    }

    /** Affiche une boîte de dialogue de confirmation. */
    private boolean confirmer(String msg){
        Alert a=new Alert(Alert.AlertType.CONFIRMATION,msg,ButtonType.OK,ButtonType.CANCEL);
        return a.showAndWait().filter(b->b==ButtonType.OK).isPresent();
    }

    // ====== CellFactories (noms des méthodes non modifiés pour override) ======
    private class MachineCell extends ListCell<Machine> {
        @Override protected void updateItem(Machine m, boolean empty){ super.updateItem(m,empty);
            if(empty||m==null){ setText(null); setContextMenu(null); return; }
            setText(m.obtenirRéférence()+" – "+m.obtenirDésignation()+(m.estAutomatique()?" (Auto)":"")+(m.estDisponible()?" (Libre)":" (Occupée)"));
            MenuItem edit=new MenuItem("Modifier"); edit.setOnAction(e->afficherDialogueModificationMachine(m));
            MenuItem del=new MenuItem("Supprimer"); del.setOnAction(e->{ if(confirmer("Supprimer machine "+m.obtenirRéférence()+" ?")){ machines.remove(m); equipements.remove(m); machineListView.refresh(); dessinerMachines(atelierCanvas.getGraphicsContext2D()); }});
            setContextMenu(new ContextMenu(edit,del));
        }
    }

    private class PosteCell extends ListCell<Poste> {
        @Override protected void updateItem(Poste p, boolean empty){ super.updateItem(p,empty);
            if(empty||p==null){ setText(null); setContextMenu(null); return;} setText(p.obtenirRéférence());
            MenuItem edit=new MenuItem("Modifier"); edit.setOnAction(e->afficherDialogueModificationPoste(p));
            MenuItem del=new MenuItem("Supprimer"); del.setOnAction(e->{ if(confirmer("Supprimer poste "+p.obtenirRéférence()+" ?")){ postes.remove(p); equipements.remove(p); posteListView.refresh(); }});
            setContextMenu(new ContextMenu(edit,del));
        }
    }

    private class OperateurCell extends ListCell<Operateur> {
        @Override protected void updateItem(Operateur o, boolean empty){ super.updateItem(o,empty);
            if(empty||o==null){ setText(null); setContextMenu(null); return;} setText(o.obtenirCode()+" – "+o.obtenirNom()+" "+o.obtenirPrénom()+(o.estOccupé()?" (Occupé)":" (Libre)"));
            MenuItem edit=new MenuItem("Modifier"); edit.setOnAction(e->afficherDialogueModificationOpérateur(o));
            MenuItem del=new MenuItem("Supprimer"); del.setOnAction(e->{ if(confirmer("Supprimer opérateur "+o.obtenirCode()+" ?")){ operateurs.remove(o); operListView.refresh(); }});
            setContextMenu(new ContextMenu(edit,del));
        }
    }

    private class GammeCell extends ListCell<Gamme> {
        @Override protected void updateItem(Gamme g, boolean empty){ super.updateItem(g,empty);
            if(empty||g==null){ setText(null); setContextMenu(null); return;} setText(g.obtenirRéférence());
            MenuItem edit=new MenuItem("Modifier"); edit.setOnAction(e->afficherDialogueModificationGamme(g));
            MenuItem del=new MenuItem("Supprimer"); del.setOnAction(e->{ if(confirmer("Supprimer gamme "+g.obtenirRéférence()+" ?")){ gammes.remove(g); operationListView.getItems().clear(); }});
            setContextMenu(new ContextMenu(edit,del));
        }
    }

    private class OperationCell extends ListCell<Operation> {
        @Override protected void updateItem(Operation op, boolean empty){ super.updateItem(op,empty);
            if(empty||op==null){ setText(null); setContextMenu(null); return;} setText(op.obtenirRéférence()+" – "+op.obtenirDésignation());
            MenuItem edit=new MenuItem("Modifier"); edit.setOnAction(e->afficherDialogueModificationOpération(op));
            MenuItem del=new MenuItem("Supprimer"); del.setOnAction(e->{ Gamme g=gammeListView.getSelectionModel().getSelectedItem(); if(g!=null&&confirmer("Supprimer opération "+op.obtenirRéférence()+" ?")){ g.obtenirOpérations().remove(op); operationListView.getItems().remove(op); mettreÀJourStatsGamme(g); }});
            setContextMenu(new ContextMenu(edit,del));
        }
    }
}
