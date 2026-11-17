package com.futbolmenajer.data.db

/**
 * GERÇEK OYUNCU VERİTABANI (2025-26 sezonu kadroları baz alınmıştır)
 * ---------------------------------------------------------------
 * Satır biçimi:  Ad Soyad;POZİSYON;DoğumYılı;Uyruk(ISO-3);Güç;Potansiyel
 * Pozisyonlar : GK CB LB RB DM CM AM LW RW ST
 *
 * Güç/potansiyel değerleri oyun dengesi için tahmini değerlerdir.
 * Kulüp adı LeagueCatalog'daki adla birebir aynı olmalıdır.
 * Kadronun geri kalanı WorldGenerator tarafından kulüp seviyesine göre üretilir.
 * Yeni oyuncu eklemek için ilgili kulübün bloğuna satır eklemen yeterli.
 */
object RealPlayers {

    val byClub: Map<String, String> = mapOf(
        // ===================== TÜRKİYE =====================
        "Galatasaray" to """
Uğurcan Çakır;GK;1996;TUR;82;84
Günay Güvenç;GK;1991;TUR;72;72
Davinson Sánchez;CB;1996;COL;80;80
Abdülkerim Bardakcı;CB;1994;TUR;78;78
Kaan Ayhan;CB;1994;TUR;76;76
Wilfried Singo;RB;2000;CIV;79;82
Eren Elmalı;LB;2000;TUR;76;78
Ismail Jakobs;LB;1999;SEN;76;77
Lucas Torreira;DM;1996;URU;81;81
Mario Lemina;DM;1993;GAB;78;78
Gabriel Sara;CM;1999;BRA;80;82
İlkay Gündoğan;CM;1990;GER;82;82
Barış Alper Yılmaz;RW;2000;TUR;80;83
Yunus Akgün;RW;2000;TUR;77;79
Roland Sallai;RW;1997;HUN;77;77
Leroy Sané;LW;1996;GER;83;83
Victor Osimhen;ST;1998;NGA;87;88
Mauro Icardi;ST;1993;ARG;80;80
""",
        "Fenerbahçe" to """
Ederson;GK;1993;BRA;84;84
Mert Günok;GK;1989;TUR;75;75
Milan Škriniar;CB;1995;SVK;82;82
Çağlar Söyüncü;CB;1996;TUR;77;77
Jayden Oosterwolde;CB;2001;NED;77;80
Nélson Semedo;RB;1993;POR;78;78
Mert Müldür;RB;1999;TUR;76;77
Archie Brown;LB;2002;ENG;75;79
Edson Álvarez;DM;1997;MEX;80;81
İsmail Yüksek;DM;1999;TUR;76;78
Fred;CM;1993;BRA;79;79
Marco Asensio;AM;1996;ESP;82;82
Anderson Talisca;AM;1994;BRA;78;78
Kerem Aktürkoğlu;LW;1998;TUR;80;81
Oğuz Aydın;LW;2000;TUR;74;77
Dorgeles Nene;RW;2002;MLI;76;80
Jhon Durán;ST;2003;COL;79;84
Youssef En-Nesyri;ST;1997;MAR;80;80
""",
        "Beşiktaş" to """
Ersin Destanoğlu;GK;2001;TUR;75;78
Tiago Djaló;CB;2000;POR;76;79
Emirhan Topçu;CB;2001;TUR;74;77
Gabriel Paulista;CB;1990;BRA;74;74
Amir Murillo;RB;1996;PAN;75;75
Rıdvan Yılmaz;LB;2001;TUR;75;78
Wilfred Ndidi;DM;1996;NGA;79;79
Orkun Kökçü;CM;2000;TUR;81;83
Salih Uçan;CM;1994;TUR;73;73
Václav Černý;RW;1997;CZE;77;77
Cengiz Ünder;RW;1997;TUR;76;76
Milot Rashica;LW;1996;KOS;74;74
El Bilal Touré;ST;2001;MLI;76;79
Tammy Abraham;ST;1997;ENG;78;78
""",
        "Trabzonspor" to """
André Onana;GK;1996;CMR;80;80
Stefan Savić;CB;1991;MNE;76;76
Arseniy Batagov;CB;2002;UKR;73;77
Wagner Pina;LB;2002;POR;73;77
Okay Yokuşlu;DM;1994;TUR;74;74
Ozan Tufan;CM;1995;TUR;74;74
Ernest Muçi;AM;2001;ALB;75;78
Edin Višća;RW;1990;BIH;72;72
Paul Onuachu;ST;1994;NGA;79;79
Felipe Augusto;ST;2003;BRA;73;78
""",
        "Başakşehir" to """
Muhammed Şengezer;GK;1997;TUR;72;74
Léo Duarte;CB;1996;BRA;74;74
Berat Özdemir;DM;1998;TUR;73;74
Olivier Kemen;CM;1996;CMR;73;73
Eldor Shomurodov;ST;1995;UZB;76;76
Davie Selke;ST;1995;GER;74;74
""",
        // ===================== İSPANYA =====================
        "Real Madrid" to """
Thibaut Courtois;GK;1992;BEL;89;89
Andriy Lunin;GK;1999;UKR;79;81
Dani Carvajal;RB;1992;ESP;82;82
Trent Alexander-Arnold;RB;1998;ENG;85;86
Antonio Rüdiger;CB;1993;GER;85;85
Éder Militão;CB;1998;BRA;84;85
Dean Huijsen;CB;2005;ESP;81;88
Raúl Asencio;CB;2003;ESP;77;82
Álvaro Carreras;LB;2003;ESP;81;86
Ferland Mendy;LB;1995;FRA;80;80
Aurélien Tchouaméni;DM;2000;FRA;85;87
Federico Valverde;CM;1998;URU;88;88
Eduardo Camavinga;CM;2002;FRA;83;87
Jude Bellingham;AM;2003;ENG;89;92
Arda Güler;AM;2005;TUR;82;89
Brahim Díaz;AM;1999;MAR;80;80
Vinícius Júnior;LW;2000;BRA;89;90
Rodrygo;RW;2001;BRA;85;87
Franco Mastantuono;RW;2007;ARG;76;88
Kylian Mbappé;ST;1998;FRA;91;92
Endrick;ST;2006;BRA;77;88
Gonzalo García;ST;2004;ESP;74;80
""",
        "Barcelona" to """
Joan García;GK;2001;ESP;83;87
Marc-André ter Stegen;GK;1992;GER;85;85
Jules Koundé;RB;1998;FRA;85;85
Pau Cubarsí;CB;2007;ESP;82;90
Ronald Araújo;CB;1999;URU;83;84
Eric García;CB;2001;ESP;79;80
Alejandro Balde;LB;2003;ESP;81;85
Marc Casadó;DM;2003;ESP;78;82
Pedri;CM;2002;ESP;88;91
Frenkie de Jong;CM;1997;NED;85;85
Gavi;CM;2004;ESP;81;87
Dani Olmo;AM;1998;ESP;84;84
Fermín López;AM;2003;ESP;80;84
Lamine Yamal;RW;2007;ESP;89;95
Raphinha;LW;1996;BRA;88;88
Marcus Rashford;LW;1997;ENG;80;80
Robert Lewandowski;ST;1988;POL;85;85
Ferran Torres;ST;2000;ESP;81;81
""",
        "Atlético Madrid" to """
Jan Oblak;GK;1993;SVN;87;87
José María Giménez;CB;1995;URU;81;81
Robin Le Normand;CB;1996;ESP;80;80
Marcos Llorente;RB;1995;ESP;82;82
Dávid Hancko;LB;1997;SVK;79;80
Koke;CM;1992;ESP;79;79
Pablo Barrios;CM;2003;ESP;80;85
Johnny Cardoso;DM;2001;USA;78;81
Álex Baena;AM;2001;ESP;83;85
Thiago Almada;AM;2001;ARG;80;82
Giuliano Simeone;RW;2002;ARG;78;82
Nico González;LW;1998;ARG;79;79
Antoine Griezmann;ST;1991;FRA;83;83
Julián Álvarez;ST;2000;ARG;87;89
Alexander Sørloth;ST;1995;NOR;81;81
""",
        "Athletic Club" to """
Unai Simón;GK;1997;ESP;84;84
Dani Vivian;CB;1999;ESP;80;81
Aymeric Laporte;CB;1994;ESP;80;80
Oihan Sancet;AM;2000;ESP;80;83
Nico Williams;LW;2002;ESP;85;88
Iñaki Williams;RW;1994;GHA;80;80
Gorka Guruzeta;ST;1996;ESP;77;77
""",
        "Villarreal" to """
Luiz Júnior;GK;2001;BRA;78;82
Juan Foyth;RB;1998;ARG;78;78
Santi Comesaña;CM;1996;ESP;77;77
Dani Parejo;CM;1989;ESP;76;76
Tajon Buchanan;RW;1999;CAN;77;77
Gerard Moreno;ST;1992;ESP;79;79
Georges Mikautadze;ST;2000;GEO;80;82
""",
        "Real Sociedad" to """
Álex Remiro;GK;1995;ESP;81;81
Igor Zubeldia;CB;1997;ESP;77;77
Brais Méndez;CM;1997;ESP;79;79
Takefusa Kubo;RW;2001;JPN;82;84
Mikel Oyarzabal;ST;1997;ESP;82;82
""",
        "Real Betis" to """
Isco;AM;1992;ESP;81;81
Antony;RW;2000;BRA;81;82
Giovani Lo Celso;CM;1996;ARG;79;79
Cucho Hernández;ST;1999;COL;78;79
""",
        // ===================== İNGİLTERE =====================
        "Manchester City" to """
Gianluigi Donnarumma;GK;1999;ITA;87;88
James Trafford;GK;2002;ENG;78;84
Rúben Dias;CB;1997;POR;86;86
Joško Gvardiol;CB;2002;CRO;85;87
Nathan Aké;CB;1995;NED;80;80
Abdukodir Khusanov;CB;2004;UZB;77;84
Rico Lewis;RB;2004;ENG;79;84
Matheus Nunes;RB;1998;POR;79;79
Nico O'Reilly;LB;2005;ENG;76;83
Rodri;DM;1996;ESP;89;89
Tijjani Reijnders;CM;1998;NED;84;85
Bernardo Silva;CM;1994;POR;84;84
Mateo Kovačić;CM;1994;CRO;81;81
Phil Foden;AM;2000;ENG;85;87
Rayan Cherki;AM;2003;FRA;82;87
Jérémy Doku;LW;2002;BEL;83;86
Savinho;RW;2004;BRA;80;85
Erling Haaland;ST;2000;NOR;91;92
Omar Marmoush;ST;1999;EGY;82;83
""",
        "Liverpool" to """
Alisson;GK;1992;BRA;88;88
Giorgi Mamardashvili;GK;2000;GEO;82;86
Virgil van Dijk;CB;1991;NED;88;88
Ibrahima Konaté;CB;1999;FRA;84;85
Joe Gomez;CB;1997;ENG;77;77
Jeremie Frimpong;RB;2000;NED;83;84
Conor Bradley;RB;2003;NIR;78;82
Milos Kerkez;LB;2003;HUN;81;85
Andrew Robertson;LB;1994;SCO;80;80
Ryan Gravenberch;DM;2002;NED;84;87
Alexis Mac Allister;CM;1998;ARG;86;86
Dominik Szoboszlai;CM;2000;HUN;84;85
Curtis Jones;CM;2001;ENG;80;81
Florian Wirtz;AM;2003;GER;88;92
Mohamed Salah;RW;1992;EGY;89;89
Federico Chiesa;RW;1997;ITA;77;77
Cody Gakpo;LW;1999;NED;83;84
Alexander Isak;ST;1999;SWE;87;88
Hugo Ekitiké;ST;2002;FRA;83;87
""",
        "Arsenal" to """
David Raya;GK;1995;ESP;87;87
Kepa Arrizabalaga;GK;1994;ESP;78;78
William Saliba;CB;2001;FRA;87;89
Gabriel Magalhães;CB;1997;BRA;87;87
Cristhian Mosquera;CB;2004;ESP;77;83
Jurriën Timber;RB;2001;NED;83;85
Ben White;RB;1997;ENG;81;81
Riccardo Calafiori;LB;2002;ITA;81;84
Piero Hincapié;LB;2002;ECU;81;84
Martín Zubimendi;DM;1999;ESP;85;86
Declan Rice;CM;1999;ENG;87;88
Mikel Merino;CM;1996;ESP;82;82
Martin Ødegaard;AM;1998;NOR;86;86
Eberechi Eze;AM;1998;ENG;84;84
Bukayo Saka;RW;2001;ENG;88;90
Noni Madueke;RW;2002;ENG;80;83
Gabriel Martinelli;LW;2001;BRA;81;82
Leandro Trossard;LW;1994;BEL;80;80
Viktor Gyökeres;ST;1998;SWE;85;85
Kai Havertz;ST;1999;GER;82;83
""",
        "Chelsea" to """
Robert Sánchez;GK;1997;ESP;80;81
Filip Jørgensen;GK;2002;DEN;76;81
Reece James;RB;1999;ENG;83;84
Malo Gusto;RB;2003;FRA;79;83
Levi Colwill;CB;2003;ENG;80;85
Trevoh Chalobah;CB;1999;ENG;79;80
Wesley Fofana;CB;2000;FRA;79;80
Marc Cucurella;LB;1998;ESP;82;82
Moisés Caicedo;DM;2001;ECU;87;88
Roméo Lavia;DM;2004;BEL;78;84
Enzo Fernández;CM;2001;ARG;85;86
Cole Palmer;AM;2002;ENG;87;90
Pedro Neto;RW;2000;POR;82;83
Estêvão;RW;2007;BRA;79;90
Alejandro Garnacho;LW;2004;ARG;80;85
Jamie Gittens;LW;2004;ENG;77;83
João Pedro;ST;2001;BRA;83;85
Liam Delap;ST;2003;ENG;78;84
""",
        "Manchester United" to """
Senne Lammens;GK;2002;BEL;78;84
Altay Bayındır;GK;1998;TUR;74;75
Matthijs de Ligt;CB;1999;NED;81;82
Harry Maguire;CB;1993;ENG;78;78
Leny Yoro;CB;2005;FRA;78;86
Lisandro Martínez;CB;1998;ARG;82;82
Diogo Dalot;RB;1999;POR;79;79
Noussair Mazraoui;RB;1997;MAR;79;79
Luke Shaw;LB;1995;ENG;77;77
Patrick Dorgu;LB;2004;DEN;76;82
Casemiro;DM;1992;BRA;78;78
Manuel Ugarte;DM;2001;URU;78;80
Kobbie Mainoo;CM;2005;ENG;79;86
Bruno Fernandes;AM;1994;POR;86;86
Matheus Cunha;AM;1999;BRA;83;84
Bryan Mbeumo;RW;1999;CMR;83;84
Amad Diallo;RW;2002;CIV;80;84
Benjamin Šeško;ST;2003;SVN;81;87
""",
        "Tottenham Hotspur" to """
Guglielmo Vicario;GK;1996;ITA;82;82
Cristian Romero;CB;1998;ARG;85;85
Micky van de Ven;CB;2001;NED;83;85
Pedro Porro;RB;1999;ESP;81;82
Destiny Udogie;LB;2002;ITA;79;83
João Palhinha;DM;1995;POR;81;81
Rodrigo Bentancur;CM;1997;URU;79;79
Yves Bissouma;DM;1996;MLI;78;78
Xavi Simons;AM;2003;NED;82;86
James Maddison;AM;1996;ENG;81;81
Mohammed Kudus;RW;2000;GHA;82;84
Brennan Johnson;RW;2001;WAL;79;81
Wilson Odobert;LW;2004;FRA;75;82
Dominic Solanke;ST;1997;ENG;80;80
Randal Kolo Muani;ST;1998;FRA;79;79
Richarlison;ST;1997;BRA;78;78
""",
        "Newcastle United" to """
Nick Pope;GK;1992;ENG;81;81
Sven Botman;CB;2000;NED;80;82
Fabian Schär;CB;1991;SUI;78;78
Malick Thiaw;CB;2001;GER;78;81
Kieran Trippier;RB;1990;ENG;77;77
Tino Livramento;RB;2002;ENG;79;83
Lewis Hall;LB;2004;ENG;79;84
Bruno Guimarães;CM;1997;BRA;86;86
Sandro Tonali;CM;2000;ITA;84;85
Joelinton;CM;1996;BRA;80;80
Anthony Elanga;RW;2002;SWE;79;82
Anthony Gordon;LW;2001;ENG;82;84
Harvey Barnes;LW;1997;ENG;77;77
Nick Woltemade;ST;2002;GER;80;84
Yoane Wissa;ST;1996;COD;79;79
""",
        "Aston Villa" to """
Emiliano Martínez;GK;1992;ARG;85;85
Ezri Konsa;CB;1997;ENG;81;81
Pau Torres;CB;1997;ESP;80;80
Matty Cash;RB;1997;POL;78;78
Lucas Digne;LB;1993;FRA;77;77
Boubacar Kamara;DM;1999;FRA;81;82
Amadou Onana;DM;2001;BEL;80;82
Youri Tielemans;CM;1997;BEL;81;81
John McGinn;CM;1994;SCO;80;80
Morgan Rogers;AM;2002;ENG;82;86
Leon Bailey;RW;1997;JAM;77;77
Ollie Watkins;ST;1995;ENG;82;82
""",
        // ===================== ALMANYA =====================
        "Bayern München" to """
Manuel Neuer;GK;1986;GER;85;85
Jonas Urbig;GK;2003;GER;74;82
Dayot Upamecano;CB;1998;FRA;84;85
Jonathan Tah;CB;1996;GER;84;84
Kim Min-jae;CB;1996;KOR;83;83
Konrad Laimer;RB;1997;AUT;81;81
Josip Stanišić;RB;2000;CRO;78;80
Alphonso Davies;LB;2000;CAN;82;84
Joshua Kimmich;DM;1995;GER;87;87
Aleksandar Pavlović;CM;2004;GER;80;86
Leon Goretzka;CM;1995;GER;81;81
Jamal Musiala;AM;2003;GER;88;92
Lennart Karl;AM;2008;GER;74;87
Michael Olise;RW;2001;FRA;87;89
Serge Gnabry;RW;1995;GER;80;80
Luis Díaz;LW;1997;COL;85;85
Harry Kane;ST;1993;ENG;90;90
Nicolas Jackson;ST;2001;SEN;80;82
""",
        "Borussia Dortmund" to """
Gregor Kobel;GK;1997;SUI;85;86
Nico Schlotterbeck;CB;1999;GER;83;85
Waldemar Anton;CB;1996;GER;81;81
Niklas Süle;CB;1995;GER;77;77
Julian Ryerson;RB;1998;NOR;78;78
Daniel Svensson;LB;2002;SWE;76;80
Ramy Bensebaini;LB;1995;ALG;76;76
Emre Can;DM;1994;GER;77;77
Felix Nmecha;CM;2000;GER;80;83
Jobe Bellingham;CM;2005;ENG;78;86
Julian Brandt;AM;1996;GER;80;80
Karim Adeyemi;LW;2002;GER;79;82
Serhou Guirassy;ST;1996;GUI;84;84
Maximilian Beier;ST;2002;GER;79;82
""",
        "Bayer Leverkusen" to """
Mark Flekken;GK;1993;NED;80;80
Edmond Tapsoba;CB;1999;BFA;81;82
Jarell Quansah;CB;2003;ENG;77;82
Alejandro Grimaldo;LB;1995;ESP;81;81
Robert Andrich;DM;1994;GER;79;79
Aleix García;CM;1997;ESP;80;80
Ibrahim Maza;CM;2005;ALG;76;84
Malik Tillman;AM;2002;USA;78;82
Ernest Poku;RW;2004;NED;77;82
Patrik Schick;ST;1996;CZE;81;81
""",
        "RB Leipzig" to """
Péter Gulácsi;GK;1990;HUN;79;79
Willi Orbán;CB;1992;HUN;80;80
Castello Lukeba;CB;2002;FRA;80;84
Xaver Schlager;CM;1997;AUT;80;80
Christoph Baumgartner;AM;1999;AUT;79;79
Antonio Nusa;LW;2005;NOR;78;86
Yan Diomande;RW;2006;CIV;76;85
""",
        "VfB Stuttgart" to """
Alexander Nübel;GK;1996;GER;80;80
Angelo Stiller;CM;2001;GER;81;84
Chris Führich;LW;1998;GER;77;77
Deniz Undav;ST;1996;GER;80;80
Ermedin Demirović;ST;1998;BIH;78;78
""",
        "Eintracht Frankfurt" to """
Kevin Trapp;GK;1990;GER;78;78
Robin Koch;CB;1996;GER;78;78
Can Uzun;AM;2005;TUR;77;85
Jonathan Burkardt;ST;2000;GER;79;81
Ritsu Doan;RW;1998;JPN;78;78
""",
        // ===================== İTALYA =====================
        "Inter" to """
Yann Sommer;GK;1988;SUI;83;83
Josep Martínez;GK;1998;ESP;77;79
Alessandro Bastoni;CB;1999;ITA;86;87
Manuel Akanji;CB;1995;SUI;83;83
Francesco Acerbi;CB;1988;ITA;79;79
Yann Bisseck;CB;2000;GER;79;82
Denzel Dumfries;RB;1996;NED;82;82
Federico Dimarco;LB;1997;ITA;83;83
Hakan Çalhanoğlu;DM;1994;TUR;85;85
Nicolò Barella;CM;1997;ITA;86;86
Henrikh Mkhitaryan;CM;1989;ARM;79;79
Petar Sučić;CM;2003;CRO;77;83
Lautaro Martínez;ST;1997;ARG;88;88
Marcus Thuram;ST;1997;FRA;85;85
Ange-Yoan Bonny;ST;2003;FRA;77;82
Francesco Pio Esposito;ST;2005;ITA;74;84
""",
        "Milan" to """
Mike Maignan;GK;1995;FRA;86;86
Fikayo Tomori;CB;1997;ENG;79;79
Strahinja Pavlović;CB;2001;SRB;78;81
Matteo Gabbia;CB;1999;ITA;78;78
Alexis Saelemaekers;RB;1999;BEL;78;78
Pervis Estupiñán;LB;1998;ECU;79;79
Youssouf Fofana;DM;1999;FRA;79;80
Luka Modrić;CM;1985;CRO;83;83
Adrien Rabiot;CM;1995;FRA;82;82
Samuele Ricci;CM;2001;ITA;79;82
Ruben Loftus-Cheek;CM;1996;ENG;78;78
Christian Pulisic;RW;1998;USA;84;84
Rafael Leão;LW;1999;POR;84;85
Santiago Giménez;ST;2001;MEX;79;81
Christopher Nkunku;ST;1997;FRA;80;80
""",
        "Juventus" to """
Michele Di Gregorio;GK;1997;ITA;80;81
Bremer;CB;1997;BRA;84;84
Federico Gatti;CB;1998;ITA;78;78
Pierre Kalulu;CB;2000;FRA;79;81
Andrea Cambiaso;LB;2000;ITA;81;83
Manuel Locatelli;DM;1998;ITA;81;81
Khéphren Thuram;CM;2001;FRA;80;83
Teun Koopmeiners;CM;1998;NED;80;80
Kenan Yıldız;LW;2005;TUR;85;91
Francisco Conceição;RW;2002;POR;81;84
Edon Zhegrova;RW;1999;KOS;80;80
Jonathan David;ST;2000;CAN;82;83
Dušan Vlahović;ST;2000;SRB;82;82
Loïs Openda;ST;2000;BEL;80;81
""",
        "Napoli" to """
Alex Meret;GK;1997;ITA;80;80
Vanja Milinković-Savić;GK;1997;SRB;80;80
Giovanni Di Lorenzo;RB;1993;ITA;81;81
Amir Rrahmani;CB;1994;KOS;81;81
Alessandro Buongiorno;CB;1999;ITA;81;83
Mathías Olivera;LB;1997;URU;78;78
Stanislav Lobotka;DM;1994;SVK;83;83
Frank Anguissa;CM;1995;CMR;82;82
Scott McTominay;CM;1996;SCO;85;85
Kevin De Bruyne;AM;1991;BEL;86;86
Matteo Politano;RW;1993;ITA;79;79
David Neres;LW;1997;BRA;80;80
Noa Lang;LW;1999;NED;79;80
Rasmus Højlund;ST;2003;DEN;80;85
Romelu Lukaku;ST;1993;BEL;81;81
""",
        "Roma" to """
Mile Svilar;GK;1999;SRB;84;86
Gianluca Mancini;CB;1996;ITA;81;81
Evan Ndicka;CB;1999;CIV;80;81
Wesley;RB;2003;BRA;78;83
Manu Koné;CM;2001;FRA;81;84
Bryan Cristante;CM;1995;ITA;78;78
Lorenzo Pellegrini;AM;1996;ITA;78;78
Paulo Dybala;AM;1993;ARG;81;81
Matías Soulé;RW;2003;ARG;80;84
Artem Dovbyk;ST;1997;UKR;79;79
""",
        "Atalanta" to """
Marco Carnesecchi;GK;2000;ITA;81;84
Odilon Kossounou;CB;2001;CIV;78;81
Ederson;CM;1999;BRA;82;82
Charles De Ketelaere;AM;2001;BEL;82;84
Ademola Lookman;LW;1997;NGA;83;83
Gianluca Scamacca;ST;1999;ITA;79;80
""",
        // ===================== FRANSA =====================
        "Paris Saint-Germain" to """
Lucas Chevalier;GK;2001;FRA;81;86
Achraf Hakimi;RB;1998;MAR;87;87
Marquinhos;CB;1994;BRA;85;85
Willian Pacho;CB;2001;ECU;84;86
Illia Zabarnyi;CB;2002;UKR;81;85
Lucas Beraldo;CB;2003;BRA;77;82
Nuno Mendes;LB;2002;POR;86;88
Vitinha;CM;2000;POR;88;89
João Neves;CM;2004;POR;85;90
Warren Zaïre-Emery;CM;2006;FRA;80;87
Fabián Ruiz;CM;1996;ESP;83;83
Ousmane Dembélé;RW;1997;FRA;90;90
Désiré Doué;RW;2005;FRA;84;91
Khvicha Kvaratskhelia;LW;2001;GEO;87;88
Bradley Barcola;LW;2002;FRA;82;85
Gonçalo Ramos;ST;2001;POR;80;81
""",
        "Marseille" to """
Gerónimo Rulli;GK;1992;ARG;80;80
Nayef Aguerd;CB;1996;MAR;79;79
Leonardo Balerdi;CB;1999;ARG;79;80
Pierre-Emile Højbjerg;DM;1995;DEN;80;80
Mason Greenwood;RW;2001;ENG;82;84
Amine Gouiri;LW;2000;ALG;80;81
Pierre-Emerick Aubameyang;ST;1989;GAB;78;78
""",
        "Monaco" to """
Denis Zakaria;DM;1996;SUI;80;80
Paul Pogba;CM;1993;FRA;76;76
Aleksandr Golovin;AM;1996;RUS;78;78
Maghnes Akliouche;AM;2002;FRA;80;84
Ansu Fati;LW;2002;ESP;75;79
Mika Biereth;ST;2003;DEN;78;82
""",
        // ===================== PORTEKİZ / HOLLANDA =====================
        "Benfica" to """
Anatoliy Trubin;GK;2001;UKR;82;85
António Silva;CB;2003;POR;80;84
Nicolás Otamendi;CB;1988;ARG;77;77
Samuel Dahl;LB;2003;SWE;76;80
Fredrik Aursnes;CM;1995;NOR;79;79
Richard Ríos;CM;2001;COL;79;82
Georgiy Sudakov;AM;2002;UKR;80;84
Vangelis Pavlidis;ST;1998;GRE;81;81
""",
        "Porto" to """
Diogo Costa;GK;1999;POR;85;86
Jakub Kiwior;CB;2000;POL;79;80
Jan Bednarek;CB;1996;POL;77;77
Alan Varela;DM;2001;ARG;80;83
Victor Froholdt;CM;2006;DEN;77;85
Gabri Veiga;CM;2002;ESP;79;82
Pepê;RW;1997;BRA;79;79
William Gomes;LW;2006;BRA;77;85
Samu Aghehowa;ST;2004;ESP;82;87
""",
        "Sporting CP" to """
Rui Silva;GK;1994;POR;80;80
Ousmane Diomande;CB;2003;CIV;81;85
Gonçalo Inácio;CB;2001;POR;80;82
Morten Hjulmand;DM;1999;DEN;82;83
Hidemasa Morita;CM;1995;JPN;79;79
Pedro Gonçalves;AM;1998;POR;81;81
Francisco Trincão;RW;1999;POR;82;82
Geovany Quenda;RW;2007;POR;78;88
Luis Suárez;ST;1997;COL;81;81
""",
        "PSV" to """
Joey Veerman;CM;1998;NED;79;79
Guus Til;AM;1997;NED;79;79
Ismael Saibari;AM;2001;MAR;79;82
Ivan Perišić;LW;1989;CRO;77;77
""",
        "Feyenoord" to """
Quinten Timber;CM;2001;NED;78;80
Anis Hadj Moussa;RW;2002;ALG;78;82
Ayase Ueda;ST;1998;JPN;78;78
""",
        "Ajax" to """
Kenneth Taylor;CM;2002;NED;78;81
Davy Klaassen;CM;1993;NED;76;76
Mika Godts;LW;2005;BEL;77;84
Wout Weghorst;ST;1992;NED;76;76
""",
        // ===================== DİĞER =====================
        "Al-Nassr" to """
Aymeric Laporte;CB;1994;ESP;80;80
Marcelo Brozović;DM;1992;CRO;80;80
João Félix;AM;1999;POR;80;80
Kingsley Coman;RW;1996;FRA;81;81
Sadio Mané;LW;1992;SEN;81;81
Cristiano Ronaldo;ST;1985;POR;84;84
""",
        "Al-Hilal" to """
Yassine Bounou;GK;1991;MAR;83;83
Kalidou Koulibaly;CB;1991;SEN;80;80
João Cancelo;RB;1994;POR;82;82
Theo Hernández;LB;1997;FRA;83;83
Rúben Neves;CM;1997;POR;82;82
Sergej Milinković-Savić;CM;1995;SRB;82;82
Malcom;RW;1997;BRA;80;80
Darwin Núñez;ST;1999;URU;81;81
""",
        "Al-Ittihad" to """
Fabinho;DM;1993;BRA;78;78
N'Golo Kanté;DM;1991;FRA;80;80
Steven Bergwijn;LW;1997;NED;78;78
Karim Benzema;ST;1987;FRA;83;83
""",
        "Al-Ahli" to """
Édouard Mendy;GK;1992;SEN;80;80
Merih Demiral;CB;1998;TUR;80;80
Franck Kessié;CM;1996;CIV;80;80
Riyad Mahrez;RW;1991;ALG;81;81
Ivan Toney;ST;1996;ENG;80;80
""",
        "Inter Miami" to """
Rodrigo De Paul;CM;1994;ARG;81;81
Lionel Messi;RW;1987;ARG;86;86
Luis Suárez;ST;1987;URU;77;77
""",
        "LAFC" to """
Denis Bouanga;LW;1994;GAB;78;78
Son Heung-min;LW;1992;KOR;83;83
"""
    )

    data class Entry(val first: String, val last: String, val pos: String, val birth: Int, val nat: String, val ovr: Int, val pot: Int)

    fun parse(club: String): List<Entry> {
        val raw = byClub[club] ?: return emptyList()
        return raw.lines().mapNotNull { line ->
            val p = line.trim().split(";")
            if (p.size < 6) return@mapNotNull null
            val name = p[0].trim()
            val idx = name.indexOf(' ')
            val first = if (idx < 0) "" else name.substring(0, idx)
            val last = if (idx < 0) name else name.substring(idx + 1)
            Entry(first, last, p[1].trim(), p[2].trim().toInt(), p[3].trim(), p[4].trim().toInt(), p[5].trim().toInt())
        }
    }
}
