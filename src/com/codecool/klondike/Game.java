package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import java.util.concurrent.TimeUnit;

import java.sql.SQLOutput;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Image restart = new Image("button/restart.png");
    private Button restartButton = new Button("", new ImageView(restart));
    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    // idea: implement with sideToDest method
    private void putCardToFoundation(Card card){
        for (Pile pile:foundationPiles){
            if (pile.isEmpty()) {
                if (card.getRank()==1){
                    card.moveToPile(pile);
                    break;
                }
            }else if (card.getSuit()==pile.getTopCard().getSuit()){
                if (card.getRank()-1==pile.getTopCard().getRank()){
                    card.moveToPile(pile);
                }
            }
        }
    }

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile.PileType type = card.getContainingPile().getPileType();
        if ((type == Pile.PileType.DISCARD || type == Pile.PileType.TABLEAU) && e.getClickCount() == 2 && card == card.getContainingPile().getTopCard()) {
            putCardToFoundation(card);
        }
        if (type == Pile.PileType.STOCK && card == card.getContainingPile().getTopCard()) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;

        if (card.isFaceDown()){
            return;
        }
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;
        draggedCards.clear();
        if (!(activePile.getTopCard()==card)){
            ObservableList<Card> cards=activePile.getCards();
            boolean notFound = true;
            for (int i =0;i<=cards.size()-1;i++){
                if (notFound){
                    if (cards.get(i)==card){
                        notFound = false;
                        draggedCards.add(cards.get(i));
                    }
                }else{
                    draggedCards.add(cards.get(i));
                }
            }
        }else{
            draggedCards.add(card);
        }
        for (Card currentcard: draggedCards){
            currentcard.getDropShadow().setRadius(20);
            currentcard.getDropShadow().setOffsetX(10);
            currentcard.getDropShadow().setOffsetY(10);

            currentcard.toFront();
            currentcard.setTranslateX(offsetX);
            currentcard.setTranslateY(offsetY);
        }

    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        List<Pile> allPiles = FXCollections.observableArrayList(tableauPiles);
        allPiles.addAll(foundationPiles);
        Pile pile = getValidIntersectingPile(card, allPiles);

        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            for (int i=0; i<=draggedCards.size()-1;i++){
                MouseUtil.slideBack(draggedCards.get(i));
            }
            draggedCards.clear();
        }
    };

    private EventHandler<MouseEvent> onRestartHandler = e -> {
        charlieFoxtrot();
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        initButton();
    };

    public boolean isGameWon() {
        //TODO
        boolean isWon = true;
        for (Pile pile : foundationPiles) {
            if(pile.getCards().size()!=13)
                isWon = false;
        }
        System.out.println("ICU");
        return isWon;
    }

    public void charlieFoxtrot() {
        getChildren().clear();
        foundationPiles.clear();
        tableauPiles.clear();
        stockPile.clear();
        discardPile.clear();
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        initButton();
    }

    public void addChangeEventHandlers(Pile pile) {
        pile.getCards().addListener(new ListChangeListener<Card>() {
            @Override
            public void onChanged(Change<? extends Card> card) {
                while (card.next()) {
                    if (card.wasAdded()) {
                        if (isGameWon()) {
                            winner();
                        }
                    }
                }
            }
        });
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
        restartButton.setOnMouseClicked(onRestartHandler);
    }

    public void refillStockFromDiscard() {
        //TODO
        if(stockPile.isEmpty()) {
            int loopNum = discardPile.getCards().size();
            for(int i = 0; i < loopNum; i++) {
                Card card = discardPile.getCards().get(discardPile.getCards().size() - 1);
                card.getContainingPile().getCards().remove(card);
                stockPile.addCard(card);
                card.flip();
            }
            discardPile.clear();
            System.out.println("Stock refilled from discard pile.");
        }
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType()== Pile.PileType.TABLEAU) {
            if (!destPile.isEmpty()) {
                if (Card.isOppositeColor(card, destPile.getTopCard())) {
                    if (card.getRank() + 1 == destPile.getTopCard().getRank())
                        return true;
                }
                return false;
            }
            if (card.getRank() == 13) {
                return true;
            }

        }else if (destPile.getPileType()== Pile.PileType.FOUNDATION){
            if (destPile.isEmpty()) {
                if (card.getRank()==1){
                    return true;
                }
                return false;
            }
            if (card.getSuit()==destPile.getTopCard().getSuit()){
                if (card.getRank()-1==destPile.getTopCard().getRank()){
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();

    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            addChangeEventHandlers(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPile.getCards().addListener(new ListChangeListener<Card>() {
                @Override
                public void onChanged(Change<? extends Card> c) {
                    while(c.next()){
                        if (c.wasRemoved()){
                            tableauPile.flipTopCard();
                        }
                    }
                }
            });
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);

        }
    }

    public void clearListeners() {
        for (int i = 0; i < 4; i++) {
            foundationPiles.get(i).clear();
        }
    }

    public void dealCards() {
        Collections.shuffle(deck);
        Iterator<Card> deckIterator = deck.iterator();
        //TODO
        for (int i =0; i<7; i++){
            for (int k =0; k<=i; k++){
                Card card = deckIterator.next();
                tableauPiles.get(i).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
            }
        }
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
        for (Pile pile:tableauPiles){
            pile.getTopCard().flip();
        }

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void initButton() {
        restartButton.setLayoutX(40);
        restartButton.setLayoutY(40);

        restartButton.setMaxSize(20, 20);
        restartButton.setMinSize(20, 20);

        restartButton.setStyle("-fx-background-color: transparent; ");
        getChildren().add(restartButton);
        System.out.println("Button set");
    }


    public void winner() {
        final Stage myDialog = new Stage();
        myDialog.initModality(Modality.APPLICATION_MODAL);
        Button okButton = new Button("CLOSE");
        Button resButton = new Button("Restart");
        okButton.setOnAction(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent arg0) {
                myDialog.close();
                clearListeners();
            }

        });
        resButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                myDialog.close();
                charlieFoxtrot();
                deck = Card.createNewDeck();
                initPiles();
                dealCards();
                initButton();
            }
        });
        Scene myDialogScene = new Scene(VBoxBuilder.create()
                .children(new Text("Gratulationz! You Won!"), okButton, resButton)
                .alignment(Pos.CENTER)
                .padding(new Insets(10))
                .build());
        myDialog.setScene(myDialogScene);
        myDialog.show();

    }



    private ObservableList<Card> gatherAllCards(){
        Pile allCardsPile = new Pile(Pile.PileType.CHEAT,"cheat",0);
        for(Pile pile:tableauPiles){
            for (Card card:pile.getCards()){
                allCardsPile.addCard(card);
            }
        }
        for(Pile pile:foundationPiles){
            for (Card card:pile.getCards()){
                allCardsPile.addCard(card);
            }
        }
        for (Card card: stockPile.getCards()){
            allCardsPile.addCard(card);
        }
        for (Card card: discardPile.getCards()){
            allCardsPile.addCard(card);
        }
        return allCardsPile.getCards();
    }



    public void solve(){
        ObservableList<Card> allCards = gatherAllCards();
        for (Card card:allCards){
            if (card.isFaceDown()){
                card.flip();
            }
        }
        int numberOfRemainingCards = 52;
        for (int i =0;i<4;i++){
            Card.CardType type;
            if (i==0){
                type = Card.CardType.diamonds;
            }else if (i==1){
                type = Card.CardType.hearts;
            }else if (i==2){
                type = Card.CardType.clubs;
            }else{
                type = Card.CardType.spades;
            }
            int currentRankToPut =1;
            for(int k=0;k<13;k++){
                for (int j=0; j<numberOfRemainingCards;j++){
                    Card currentCard = allCards.get(j);
                    if (currentCard.getRank()==currentRankToPut && currentCard.getSuit()==type){
                        draggedCards.add(currentCard);
                        MouseUtil.slideToDest(draggedCards,foundationPiles.get(i));
                        draggedCards.clear();
                        currentRankToPut++;
                        break;
                    }
                }
            }
        }

    }
}
