package com.example.viergewinntbeta;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

public class VierGewinntView extends View {

    private Context context;

    private enum ZellenZustand {
        SPIELER(Color.GREEN), COMPUTER(Color.DKGRAY), LEER(Color.TRANSPARENT);
        private int color;

        ZellenZustand(int c) {
            color = c;
        }

        int getColor() {
            return color;
        }
    }

    ;
    private int abstandRand = 50;
    private int abstandOben = 400;
    private int margin = 10;
    private final int ANZ_ZEILEN = 6;
    private final int ANZ_SPALTEN = 7;
    private int zellGroesse;
    private int pfeilHoehe;
    private int pfeilYAbstand;
    boolean freigabeSpieler;
    private int kiStaerke = 1;

    int nummerZug;


    private ZellenZustand[][] spielFeld;
    private Bitmap rechteck;
    private Bitmap pfeil;
    private Paint rectPaint;
    private Paint pfeilPaint;
    private Paint kreisPaint;
    private Paint siegMarkierungPaint;
    private Rect rechteckDst;
    private Rect pfeilDst;
    private Random zufallszahlenGenerator;
    private Spielergebnis gewinnerInfo;
    private boolean infoGezeigt = false;

    public VierGewinntView(Context context) {
        super(context);
        this.context = context;
        setBackgroundResource(R.drawable.hintergrund);

        // Symbole laden für die Leinwand
        Resources res = getResources();
        rechteck = BitmapFactory.decodeResource(res, R.drawable.zelle);
        rechteckDst = new Rect();
        pfeil = BitmapFactory.decodeResource(res, R.drawable.pfeilunten);
        pfeilDst = new Rect();
        // Zeichenstile
        rectPaint = new Paint();
        pfeilPaint = new Paint();
        siegMarkierungPaint = new Paint();
        siegMarkierungPaint.setColor(Color.GREEN);
        siegMarkierungPaint.setStrokeWidth(15);
        siegMarkierungPaint.setStyle(Paint.Style.STROKE);
        // Zeichenstil für die Öffnungen
        kreisPaint = new Paint();
        kreisPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // Zellen initalisieren
        spielFeld = new ZellenZustand[ANZ_ZEILEN][ANZ_SPALTEN];
        zufallszahlenGenerator = new Random(System.currentTimeMillis());
        datenzuruecksetzen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        abstandRand = getWidth() / 20;
        zellGroesse = (getWidth() - 2 * abstandRand) / ANZ_SPALTEN - 1;
        abstandOben = getHeight() / 4;
        margin = getWidth() / 100;
        pfeilYAbstand = abstandOben - pfeil.getHeight() - 2 * margin;
        pfeilHoehe = zellGroesse * 5 / 4;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Spielgerüst zeichnen
        // Achtung: Spalten zuerst
        if (hasWindowFocus()) {
            for (int j = 0; j < ANZ_SPALTEN; j++) {
                // Pfeile nur zeichnen, wenn Spieler dran ist
                if (freigabeSpieler) {
                    pfeilDst.set(abstandRand + j * zellGroesse + margin / 2, pfeilYAbstand, abstandRand + (j + 1) * zellGroesse - margin / 2, pfeilYAbstand + pfeilHoehe);
                    canvas.drawBitmap(pfeil, null, pfeilDst, pfeilPaint);
                }
                for (int i = 0; i < ANZ_ZEILEN; i++) {
                    rechteckDst.set(abstandRand + j * zellGroesse, abstandOben + i * zellGroesse, abstandRand + (j + 1) * zellGroesse, abstandOben + (i + 1) * zellGroesse);
                    canvas.drawBitmap(rechteck, null, rechteckDst, rectPaint);
                    kreisPaint.setColor(spielFeld[i][j].getColor());
                    canvas.drawCircle(abstandRand + j * zellGroesse + zellGroesse / 2, abstandOben + i * zellGroesse + zellGroesse / 2, (zellGroesse - margin) / 2, kreisPaint);
                }
            }

            if (this.gewinnerInfo != null) {
                // siegreiche Linie zeichnen
                int startX = abstandRand + zellGroesse / 2 + gewinnerInfo.leseStartY() * zellGroesse;
                int startY = abstandOben + zellGroesse / 2 + gewinnerInfo.leseStartX() * zellGroesse;
                int endX = abstandRand + zellGroesse / 2 + gewinnerInfo.leseEndY() * zellGroesse;
                int endY = abstandOben + zellGroesse / 2 + gewinnerInfo.leseEndX() * zellGroesse;
                canvas.drawLine(startX, startY, endX, endY, siegMarkierungPaint);
            }
        }
    }

    // Spielerzug
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        float xPos, yPos;
        int zeile;
        int pfeilGeklickt;

        if (action == MotionEvent.ACTION_DOWN)
            return true;
        else if (action == MotionEvent.ACTION_UP) {
            // losgelassen -> Eingabe ermitteln
            xPos = event.getX();
            yPos = event.getY();
            // Toast.makeText(context, "Punkt: ("+xPos+"//"+yPos+")", Toast.LENGTH_SHORT).show();
            // auswerten
            if (freigabeSpieler && gewinnerInfo == null) {
                if (yPos > pfeilYAbstand && yPos < pfeilYAbstand + pfeil.getHeight()) {
                    // auf Pfeilhöhe geklickt
                    // Spalte ermitteln
                    pfeilGeklickt = (int) (xPos - abstandRand) / zellGroesse;
                    // Zeile ermitteln
                    zeile = niedrigsteFreieZeileErmitteln(pfeilGeklickt);
                    if (spielFeld[zeile][pfeilGeklickt] == ZellenZustand.LEER) {
                        // gültiger Zug liegt vor
                        // Zelle neue Farbe zuweisen
                        spielFeld[zeile][pfeilGeklickt] = ZellenZustand.SPIELER;
                        // neu zeichnen
                        invalidate();
                        Spielergebnis ergebnis = bestimmeGewinner();
                        if (ergebnis.leseZustand() == ZellenZustand.SPIELER) {
                            // Spieler hat gewonnen
                            gewinnerInfo = ergebnis;
                            zeigeErgebnis();
                        } else {
                            // Computerzug aufrufen
                            freigabeSpieler = false;
                            macheComputerZug();
                        }
                    }

                } else if (!infoGezeigt) {
                    Toast.makeText(context, "Klicke auf einen Pfeil!", Toast.LENGTH_SHORT).show();
                    infoGezeigt = true;
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    int niedrigsteFreieZeileErmitteln(int spalte) {
        int zeile = ANZ_ZEILEN - 1;
        while (spielFeld[zeile][spalte] != ZellenZustand.LEER && zeile > 0) {
            zeile--;
        }
        return zeile;
    }

    // TODO: 08.06.2020  
    private class Spielergebnis {
        private ZellenZustand zustand;
        private int startX;
        private int startY;
        private int endX;
        private int endY;

        public Spielergebnis(ZellenZustand zustand, int startX, int startY, int endX, int endY) {
            this.zustand = zustand;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        public Spielergebnis(ZellenZustand zustand) {
            this.zustand = zustand;
            startX = -1;
            startY = -1;
            endX = -1;
            endY = -1;
        }

        public ZellenZustand leseZustand() {
            return zustand;
        }

        public int leseStartX() {
            return startX;
        }

        public int leseStartY() {
            return startY;
        }

        public int leseEndX() {
            return endX;
        }

        public int leseEndY() {
            return endY;
        }
    }

    public void datenzuruecksetzen() {
        for (int i = 0; i < ANZ_ZEILEN; i++) {
            for (int j = 0; j < ANZ_SPALTEN; j++) {
                spielFeld[i][j] = ZellenZustand.LEER;
            }
        }
        gewinnerInfo = null;
        nummerZug = 0;
        invalidate();
    }

    public void starteNeu(boolean spielerBeginnt) {
        datenzuruecksetzen();
        freigabeSpieler = spielerBeginnt;

        if (!freigabeSpieler)
            macheComputerZug();
    }

    private void macheComputerZug() {
        if (!freigabeSpieler) {
            AndroidSpielzugTask androidZug = new AndroidSpielzugTask();
            androidZug.execute();
        }
    }

    private class AndroidSpielzugTask extends AsyncTask<Void, Void, Boolean> {
        private int anzahlFreieFelder;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            anzahlFreieFelder = bestimmeAnzahlFreieFelder();
        }

        // den Android Zug berechnen
        protected Boolean doInBackground(Void... args) {

            if (anzahlFreieFelder > 0 && gewinnerInfo == null) {
                // ein bisschen warten, damit der Spieler nicht überrumpelt wird
                try {
                    Thread.sleep(500); // 0,5 sec.
                } catch (Exception ex) {
                }

                switch (kiStaerke) {
                    case 1:
                        kiStaerke1(); break;
                    case 2:
                        kiStaerke2(); break;
                    default:
                }
            }

            return true; // damit onPostExecute() aktiviert wird
        }

        // Zug anzeigen
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Spielergebnis ergebnis = bestimmeGewinner();
            int anzahlFrei = bestimmeAnzahlFreieFelder();
            freigabeSpieler = true;
            invalidate();

            if (ergebnis.leseZustand() != ZellenZustand.LEER || anzahlFrei == 0) {
                zeigeErgebnis();
            }
        }

        void kiStaerke1() {
            int x;
            int y;

            while (true) {
                y = zufallszahlenGenerator.nextInt(ANZ_SPALTEN);
                x = niedrigsteFreieZeileErmitteln(y);
                if (spielFeld[x][y] == ZellenZustand.LEER) {
                    // leeres Feld gefunden: raus aus der while Schleife
                    break;
                }
            }
            spielFeld[x][y] = ZellenZustand.COMPUTER;
        }


        void kiStaerke2() {
            int x;
            int y;
            boolean matchballSpieler = false;
            boolean matchballComputer = false;

            y = erkenneMatchball(ZellenZustand.COMPUTER);
            if (y >= 0)
                matchballComputer = true;

            if (!matchballComputer) {
                y = erkenneMatchball(ZellenZustand.SPIELER);
                if (y >= 0)
                    matchballSpieler = true;
            }
            if (!matchballComputer && !matchballSpieler) {
                kiStaerke1();
            } else {
                x = niedrigsteFreieZeileErmitteln(y);
                spielFeld[x][y] = ZellenZustand.COMPUTER;
            }
        }
    }

    int bestimmeAnzahlFreieFelder() {
        int zaehler = 0;
        for (int i = 0; i < ANZ_ZEILEN; i++) {
            for (int j = 0; j < ANZ_SPALTEN; j++) {
                if (spielFeld[i][j] == ZellenZustand.LEER)
                    zaehler++;
            }
        }
        return zaehler;
    }

    private Spielergebnis bestimmeGewinner() {

        ZellenZustand zustand;

        // Horizontalen testen
        for (int i = 0; i < ANZ_ZEILEN; i++)
            for (int j = 0; j < ANZ_SPALTEN - 3; j++) {
                zustand = spielFeld[i][j];
                if (zustand != ZellenZustand.LEER)
                    if (vierGleiche(spielFeld[i][j], spielFeld[i][j + 1], spielFeld[i][j + 2], spielFeld[i][j + 3]))
                        return new Spielergebnis(zustand, i, j, i, j + 3);
            }
        // Vertikalen testen
        for (int j = 0; j < ANZ_SPALTEN; j++)
            for (int i = 0; i < ANZ_ZEILEN - 3; i++) {
                zustand = spielFeld[i][j];
                if (zustand != ZellenZustand.LEER)
                    if (vierGleiche(spielFeld[i][j], spielFeld[i + 1][j], spielFeld[i + 2][j], spielFeld[i + 3][j]))
                        return new Spielergebnis(zustand, i, j, i + 3, j);
            }
        // Diagonalen testen
        // das Gesamtfeld wird in 12 4x4 Matrizen aufgeteilt und so vollständig abgebildet.
        // in allen 4x4 Matrizen jeweils beide Diag. testen
        for (int i = 0; i < ANZ_ZEILEN - 3; i++)
            for (int j = 0; j < ANZ_SPALTEN - 3; j++) {
                // erst die Diag. von links oben nach rechts unten
                zustand = spielFeld[i][j];
                if (zustand != ZellenZustand.LEER)
                    if (vierGleiche(spielFeld[i][j], spielFeld[i + 1][j + 1], spielFeld[i + 2][j + 2], spielFeld[i + 3][j + 3]))
                        return new Spielergebnis(zustand, i, j, i + 3, j + 3);
                // dann von links unten nach rechts oben
                zustand = spielFeld[i + 3][j];
                if (zustand != ZellenZustand.LEER)
                    if (vierGleiche(spielFeld[i + 3][j], spielFeld[i + 2][j + 1], spielFeld[i + 1][j + 2], spielFeld[i][j + 3]))
                        return new Spielergebnis(zustand, i + 3, j, i, j + 3);
            }

        return new Spielergebnis(ZellenZustand.LEER);
    }

    private int erkenneMatchball(ZellenZustand farbe) {

        int zwischen;

        // Horizontalen testen
        for (int i = 0; i < ANZ_ZEILEN; i++)
            for (int j = 0; j < ANZ_SPALTEN - 3; j++) {
                zwischen = dreiv4Gleiche(farbe, spielFeld[i][j], spielFeld[i][j + 1], spielFeld[i][j + 2], spielFeld[i][j + 3]);
                if (zwischen != -1)
                    if (i == niedrigsteFreieZeileErmitteln(j + zwischen))
                        return j + zwischen;
            }
        // Vertikalen testen
        for (int j = 0; j < ANZ_SPALTEN; j++)
            for (int i = 0; i < ANZ_ZEILEN - 3; i++) {
                zwischen = dreiv4Gleiche(farbe, spielFeld[i][j], spielFeld[i + 1][j], spielFeld[i + 2][j], spielFeld[i + 3][j]);
                if (zwischen != -1)
                    return j;
            }
        // Diagonalen testen
        // das Gesamtfeld wird in 12 4x4 Matrizen aufgeteilt und so vollständig abgebildet.
        // in allen 4x4 Matrizen jeweils beide Diag. testen
        for (int i = 0; i < ANZ_ZEILEN - 3; i++)
            for (int j = 0; j < ANZ_SPALTEN - 3; j++) {
                // erst die Diag. von links oben nach rechts unten
                zwischen = dreiv4Gleiche(farbe, spielFeld[i][j], spielFeld[i + 1][j + 1], spielFeld[i + 2][j + 2], spielFeld[i + 3][j + 3]);
                if (zwischen != -1)
                    if ((i + zwischen) == niedrigsteFreieZeileErmitteln(j + zwischen))
                        return j + zwischen;
                // dann von links unten nach rechts oben
                zwischen = dreiv4Gleiche(farbe, spielFeld[i + 3][j], spielFeld[i + 2][j + 1], spielFeld[i + 1][j + 2], spielFeld[i][j + 3]);
                if (zwischen != -1)
                    if ((i + 3 - zwischen) == niedrigsteFreieZeileErmitteln(j + zwischen))
                        return j + zwischen;
            }

        return -1;
    }

    private void zeigeErgebnis() {

        if (gewinnerInfo == null) {
            gewinnerInfo = bestimmeGewinner();
        }

        String nachricht;
        Resources ressourcen = getResources();

        switch (gewinnerInfo.leseZustand()) {
            case COMPUTER:
                nachricht = ressourcen.getText(R.string.androidGewinner).toString();
                break;
            case SPIELER:
                nachricht = ressourcen.getText(R.string.spielerGewinner).toString();
                break;
            default:
                nachricht = ressourcen.getText(R.string.unentschieden).toString();
                break;
        }

        // grafische Anzeige der Gewinnreihe
        if (gewinnerInfo.leseZustand() != ZellenZustand.LEER)
            invalidate();

        Toast.makeText(context, nachricht, Toast.LENGTH_SHORT).show();
    }

    public boolean vierGleiche(ZellenZustand z1, ZellenZustand z2, ZellenZustand z3, ZellenZustand z4) {
        return z1 == z2 && z2 == z3 && z3 == z4;
    }

    public int dreiv4Gleiche(ZellenZustand soll, ZellenZustand z1, ZellenZustand z2, ZellenZustand z3, ZellenZustand z4) {
        // Rückgabewert ist die Stelle, wo nicht gleich (index 0..3), sonst -1
        if (soll == z2 && z2 == z3 && z3 == z4 && z1 == ZellenZustand.LEER)
            return 0;
        if (soll == z1 && z1 == z3 && z3 == z4 && z2 == ZellenZustand.LEER)
            return 1;
        if (soll == z1 && z1 == z2 && z2 == z4 && z3 == ZellenZustand.LEER)
            return 2;
        if (soll == z1 && z1 == z2 && z2 == z3 && z4 == ZellenZustand.LEER)
            return 3;
        return -1;
    }

    public void setKiStaerke(int index) {
        kiStaerke = index;
    }
}
