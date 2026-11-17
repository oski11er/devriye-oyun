package com.futbolmenajer.data.db

/**
 * 2026-27 SEZONU GÜNCEL KADROLAR (Kasım 2026 itibarıyla, kamuya açık kulüp kadro listelerinden derlendi)
 * Biçim: Ad Soyad;POZ;DoğumYılı;UYRUK;Güç;Potansiyel
 * Güç/potansiyel değerleri bu oyuna özgü tahminlerdir (lisanslı oyun verisi DEĞİLDİR).
 * Bu dosyadaki bir oyuncu, RealPlayers.kt içindeki eski kulübünden otomatik olarak silinir.
 */
object RealPlayers2026 {

    /** Kulüp → teknik direktör (2026-27). */
    val headCoaches: Map<String, Pair<String, String>> = mapOf(
        "Galatasaray" to ("Okan Buruk" to "TUR"), "Fenerbahçe" to ("İsmail Kartal" to "TUR"), "Beşiktaş" to ("Vincenzo Italiano" to "ITA"),
        "Trabzonspor" to ("Thomas Reis" to "GER"), "Başakşehir" to ("Nuri Şahin" to "TUR"), "Samsunspor" to ("Thorsten Fink" to "GER"),
        "Göztepe" to ("Stanimir Stoilov" to "BUL"), "Kasımpaşa" to ("Emre Belözoğlu" to "TUR"), "Kocaelispor" to ("Selçuk İnan" to "TUR"),
        "Alanyaspor" to ("João Pereira" to "POR"), "Amedspor" to ("Besnik Hasi" to "ALB"), "Çorum FK" to ("Uğur Uçar" to "TUR"),
        "Erzurumspor" to ("Serkan Özbalta" to "TUR"), "Eyüpspor" to ("Özhan Pulat" to "TUR"), "Gaziantep FK" to ("Mirel Rădoi" to "ROU"),
        "Gençlerbirliği" to ("Metin Diyadin" to "TUR"), "Konyaspor" to ("İlhan Palut" to "TUR"), "Çaykur Rizespor" to ("Recep Uçar" to "TUR"),
        "Arsenal" to ("Mikel Arteta" to "ESP"), "Aston Villa" to ("Unai Emery" to "ESP"), "Bournemouth" to ("Marco Rose" to "GER"),
        "Brentford" to ("Keith Andrews" to "IRL"), "Brighton" to ("Fabian Hürzeler" to "GER"), "Chelsea" to ("Xabi Alonso" to "ESP"),
        "Coventry City" to ("Frank Lampard" to "ENG"), "Crystal Palace" to ("Pierre Sage" to "FRA"), "Everton" to ("David Moyes" to "SCO"),
        "Fulham" to ("Álvaro Arbeloa" to "ESP"), "Hull City" to ("Sergej Jakirović" to "CRO"), "Ipswich Town" to ("Gary O'Neil" to "ENG"),
        "Leeds United" to ("Daniel Farke" to "GER"), "Liverpool" to ("Andoni Iraola" to "ESP"), "Manchester City" to ("Enzo Maresca" to "ITA"),
        "Manchester United" to ("Mauricio Pochettino" to "ARG"), "Newcastle United" to ("Matthias Jaissle" to "GER"),
        "Nottingham Forest" to ("Oliver Glasner" to "AUT"), "Sunderland" to ("Régis Le Bris" to "FRA"), "Tottenham Hotspur" to ("Roberto De Zerbi" to "ITA"),
        "Alavés" to ("Quique Sánchez Flores" to "ESP"), "Athletic Club" to ("Edin Terzić" to "GER"), "Atlético Madrid" to ("Diego Simeone" to "ARG"),
        "Barcelona" to ("Hansi Flick" to "GER"), "Celta Vigo" to ("Claudio Giráldez" to "ESP"), "Deportivo" to ("Antonio Hidalgo" to "ESP"),
        "Elche" to ("Martín Anselmi" to "ARG"), "Espanyol" to ("Manolo González" to "ESP"), "Getafe" to ("José Bordalás" to "ESP"),
        "Levante" to ("Luís Castro" to "POR"), "Málaga" to ("Juan Francisco Funes" to "ESP"), "Osasuna" to ("Luis Miguel Ramis" to "ESP"),
        "Racing Santander" to ("José Alberto López" to "ESP"), "Rayo Vallecano" to ("Beñat San José" to "ESP"), "Real Betis" to ("Manuel Pellegrini" to "CHI"),
        "Real Madrid" to ("José Mourinho" to "POR"), "Real Sociedad" to ("Pellegrino Matarazzo" to "USA"), "Sevilla" to ("Luis García" to "ESP"),
        "Valencia" to ("Óscar Sánchez" to "ESP"), "Villarreal" to ("Iñigo Pérez" to "ESP"),
        "Atalanta" to ("Maurizio Sarri" to "ITA"), "Bologna" to ("Raffaele Palladino" to "ITA"), "Cagliari" to ("Fabio Pisacane" to "ITA"),
        "Como" to ("Cesc Fàbregas" to "ESP"), "Fiorentina" to ("Paolo Vanoli" to "ITA"), "Frosinone" to ("Massimiliano Alvini" to "ITA"),
        "Genoa" to ("Daniele De Rossi" to "ITA"), "Inter" to ("Cristian Chivu" to "ROU"), "Juventus" to ("Luciano Spalletti" to "ITA"),
        "Lazio" to ("Gennaro Gattuso" to "ITA"), "Lecce" to ("Eusebio Di Francesco" to "ITA"), "Milan" to ("Ruben Amorim" to "POR"),
        "Monza" to ("Ivan Jurić" to "CRO"), "Napoli" to ("Massimiliano Allegri" to "ITA"), "Parma" to ("Alberto Gilardino" to "ITA"),
        "Roma" to ("Gian Piero Gasperini" to "ITA"), "Sassuolo" to ("Alberto Aquilani" to "ITA"), "Torino" to ("Ignazio Abate" to "ITA"),
        "Udinese" to ("Paolo Zanetti" to "ITA"), "Venezia" to ("Davide Nicola" to "ITA"),
        "FC Augsburg" to ("Manuel Baum" to "GER"), "1. FC Union Berlin" to ("Mauro Lustrinelli" to "SUI"), "Werder Bremen" to ("Daniel Thioune" to "GER"),
        "Borussia Dortmund" to ("Niko Kovač" to "CRO"), "SV Elversberg" to ("Vincent Wagner" to "GER"), "Eintracht Frankfurt" to ("Adi Hütter" to "AUT"),
        "SC Freiburg" to ("Julian Schuster" to "GER"), "Hamburger SV" to ("Merlin Polzin" to "GER"), "TSG Hoffenheim" to ("Christian Ilzer" to "AUT"),
        "1. FC Köln" to ("René Wagner" to "GER"), "RB Leipzig" to ("Martín Demichelis" to "ARG"), "Bayer Leverkusen" to ("Carles Martínez Novell" to "ESP"),
        "1. FSV Mainz 05" to ("Urs Fischer" to "SUI"), "Borussia Mönchengladbach" to ("Eugen Polanski" to "GER"), "Bayern München" to ("Vincent Kompany" to "BEL"),
        "SC Paderborn 07" to ("Ralf Kettemann" to "GER"), "FC Schalke 04" to ("Miron Muslić" to "AUT"), "VfB Stuttgart" to ("Sebastian Hoeneß" to "GER"),
        "Angers" to ("Stéphane Gilli" to "FRA"), "Auxerre" to ("Will Still" to "BEL"), "Brest" to ("Julien Lachuer" to "FRA"), "Le Havre" to ("Didier Digard" to "FRA"),
        "Le Mans" to ("Patrick Videira" to "FRA"), "Lens" to ("Yannick Cahuzac" to "FRA"), "Lille" to ("Davide Ancelotti" to "ITA"), "Lorient" to ("Alexandre Dujeux" to "FRA"),
        "Lyon" to ("Paulo Fonseca" to "POR"), "Marseille" to ("Bruno Genesio" to "FRA"), "Monaco" to ("Filipe Luís" to "BRA"), "Nice" to ("Olivier Pantaloni" to "FRA"),
        "Paris FC" to ("Liam Rosenior" to "ENG"), "Paris Saint-Germain" to ("Luis Enrique" to "ESP"), "Rennes" to ("Franck Haise" to "FRA"),
        "Strasbourg" to ("Hugo Oliveira" to "POR"), "Toulouse" to ("Jens Berthel Askou" to "DEN"), "Troyes" to ("Stéphane Dumont" to "FRA"),
        "Benfica" to ("Marco Silva" to "POR")
    )

    val byClub: Map<String, String> = mapOf(
        "Galatasaray" to """
Uğurcan Çakır;GK;1996;TUR;83;84
Günay Güvenç;GK;1991;TUR;71;71
Jankat Yılmaz;GK;2004;TUR;62;72
El Chadaille Bitshiabu;CB;2005;FRA;74;83
Davinson Sánchez;CB;1996;COL;80;80
Abdülkerim Bardakcı;CB;1994;TUR;78;78
Kaan Ayhan;CB;1994;TUR;76;76
Arda Ünyay;CB;2007;TUR;64;82
Wilfried Singo;RB;2000;CIV;80;82
Ismail Jakobs;LB;1999;SEN;76;77
Eren Elmalı;LB;2000;TUR;76;77
Kazımcan Karataş;LB;2003;TUR;70;75
Lucas Torreira;DM;1996;URU;80;80
Mario Lemina;DM;1993;GAB;77;77
Lesley Ugochukwu;DM;2004;FRA;75;82
Eyüp Aydın;DM;2004;TUR;69;76
Gabriel Sara;CM;1999;BRA;81;82
İlkay Gündoğan;CM;1990;GER;80;80
Renato Nhaga;CM;2007;GNB;68;84
Aleksey Batrakov;AM;2005;RUS;78;87
Leroy Sané;RW;1996;GER;82;82
Yunus Akgün;RW;2000;TUR;77;78
Roland Sallai;RW;1997;HUN;77;77
Armando Güner;RW;2008;ARG;63;85
Rafael Leão;LW;1999;POR;85;85
Barış Alper Yılmaz;LW;2000;TUR;80;82
Berat Luş;LW;2007;TUR;62;78
Victor Osimhen;ST;1998;NGA;88;88
Deniz Gül;ST;2004;TUR;74;81
Ada Yüzgeç;ST;2009;TUR;58;83
""",
        "Fenerbahçe" to """
Ederson;GK;1993;BRA;84;84
Mert Günok;GK;1989;TUR;74;74
Tarık Çetin;GK;1997;TUR;68;70
Milan Škriniar;CB;1995;SVK;82;82
Nathan Aké;CB;1995;NED;80;80
Jayden Oosterwolde;CB;2001;NED;78;80
Rodrigo Becão;CB;1996;BRA;75;75
Yiğit Efe Demir;CB;2004;TUR;70;78
Ognjen Mimović;CB;2004;SRB;71;77
Levent Mercan;LB;2000;TUR;73;74
Archie Brown;LB;2002;ENG;76;79
Nélson Semedo;RB;1993;POR;77;77
Mert Müldür;RB;1999;TUR;76;77
Kojo Peprah Oppong;RB;2004;GHA;71;79
N'Golo Kanté;DM;1991;FRA;80;80
İsmail Yüksek;DM;1999;TUR;76;77
Mattéo Guendouzi;CM;1999;FRA;81;81
Mert Hakan Yandaş;CM;1994;TUR;72;72
Bartuğ Elmaz;CM;2003;TUR;68;75
Marco Asensio;AM;1996;ESP;81;81
İrfan Can Kahveci;AM;1995;TUR;75;75
Kerem Aktürkoğlu;LW;1998;TUR;80;80
Oğuz Aydın;LW;2000;TUR;74;76
Mason Greenwood;RW;2001;ENG;83;84
Dorgeles Nene;ST;2002;MLI;77;81
Romelu Lukaku;ST;1993;BEL;79;79
Vedat Muriqi;ST;1994;KOS;79;79
""",
        "Beşiktaş" to """
Alexander Nübel;GK;1996;GER;80;80
Doğan Alemdar;GK;2002;TUR;70;75
Emir Yaşar;GK;2006;TUR;60;72
Emmanuel Agbadou;CB;1997;CIV;77;78
Tiago Djaló;CB;2000;POR;76;78
Kassoum Ouattara;CB;2004;FRA;72;79
Emirhan Topçu;CB;2001;TUR;74;76
Ümit Akdağ;CB;2003;TUR;70;76
Taylan Bulut;RB;2006;GER;68;80
Amir Murillo;RB;1996;PAN;75;75
Rıdvan Yılmaz;LB;2001;TUR;74;76
Wilfred Ndidi;DM;1996;NGA;79;79
Salih Özcan;DM;1998;TUR;75;75
Orkun Kökçü;CM;2000;TUR;82;83
Fabio Miretti;CM;2003;ITA;74;79
Kartal Yılmaz;CM;2001;TUR;70;74
Junior Olaitan;AM;2002;BEN;72;77
Václav Černý;RW;1997;CZE;77;77
Leandro Trossard;LW;1994;BEL;80;80
Milot Rashica;LW;1996;KOS;73;73
Ernest Poku;RW;2004;NED;76;82
Dušan Vlahović;ST;2000;SRB;82;82
Oh Hyeon-gyu;ST;2001;KOR;75;77
Semih Kılıçsoy;ST;2005;TUR;72;81
Mustafa Hekimoğlu;ST;2007;TUR;63;79
""",
        "Trabzonspor" to """
André Onana;GK;1996;CMR;80;80
Ahmet Doğan Yıldırım;GK;2005;TUR;62;73
Stefan Savić;CB;1991;MNE;75;75
Samet Akaydin;CB;1994;TUR;74;74
Arseniy Batahov;CB;2002;UKR;74;78
Chibuike Nwaiwu;CB;2003;NGA;73;79
Cenk Özkacar;CB;2000;TUR;72;74
Wagner Pina;LB;2002;CPV;74;78
Sidny Lopes Cabral;LB;2002;CPV;70;75
Fabinho;DM;1993;BRA;77;77
Okay Yokuşlu;DM;1994;TUR;73;73
Batista Mendy;CM;2000;FRA;74;77
Ozan Tufan;CM;1995;TUR;73;73
Benjamin Bouchouari;CM;2001;MAR;73;77
Tim Jabol-Folcarelli;CM;2000;FRA;72;74
Ruslan Malinovskyi;AM;1993;UKR;77;77
Ernest Muçi;AM;2001;ALB;75;78
Mohamed Salah;RW;1992;EGY;85;85
Mustafa Eskihellaç;RW;1997;TUR;70;70
Paul Onuachu;ST;1994;NGA;78;78
Franculino;ST;2004;GNB;72;80
Umut Nayir;ST;1993;TUR;70;70
""",
        "Real Madrid" to """
Thibaut Courtois;GK;1992;BEL;88;88
Andriy Lunin;GK;1999;UKR;79;80
Trent Alexander-Arnold;RB;1998;ENG;85;86
Denzel Dumfries;RB;1996;NED;82;82
Antonio Rüdiger;CB;1993;GER;83;83
Éder Militão;CB;1998;BRA;83;83
Dean Huijsen;CB;2005;ESP;83;89
Ibrahima Konaté;CB;1999;FRA;85;86
Raúl Asencio;CB;2003;ESP;78;82
Álvaro Carreras;LB;2003;ESP;82;86
Marc Cucurella;LB;1998;ESP;83;83
Ferland Mendy;LB;1995;FRA;78;78
Aurélien Tchouaméni;DM;2000;FRA;85;87
Federico Valverde;CM;1998;URU;88;88
Eduardo Camavinga;CM;2002;FRA;83;86
Thiago Pitarch;CM;2007;ESP;66;84
Jude Bellingham;AM;2003;ENG;89;92
Arda Güler;AM;2005;TUR;84;90
Bernardo Silva;AM;1994;POR;83;83
Brahim Díaz;AM;1999;MAR;80;80
Vinícius Júnior;LW;2000;BRA;89;89
Yan Diomande;LW;2006;CIV;78;88
Rodrygo;RW;2001;BRA;84;86
Kylian Mbappé;ST;1998;FRA;92;92
Endrick;ST;2006;BRA;78;88
Carlos Espí;ST;2005;ESP;68;81
""",
        "Barcelona" to """
Joan García;GK;2001;ESP;85;88
Wojciech Szczęsny;GK;1990;POL;79;79
Jules Koundé;RB;1998;FRA;85;85
Ronald Araújo;CB;1999;URU;82;82
Pau Cubarsí;CB;2007;ESP;84;91
Gerard Martín;CB;2002;ESP;77;80
Eric García;CB;2001;ESP;79;80
Alejandro Balde;LB;2003;ESP;81;84
Marc Casadó;DM;2003;ESP;79;82
Marc Bernal;DM;2007;ESP;74;87
Pedri;CM;2002;ESP;89;91
Frenkie de Jong;CM;1997;NED;85;85
Gavi;CM;2004;ESP;82;87
Dani Olmo;AM;1998;ESP;83;83
Fermín López;AM;2003;ESP;82;85
Lamine Yamal;RW;2007;ESP;91;96
Roony Bardghji;RW;2005;SWE;72;83
Raphinha;LW;1996;BRA;88;88
Anthony Gordon;LW;2001;ENG;83;84
Ferran Torres;ST;2000;ESP;81;81
""",
        "Atlético Madrid" to """
Jan Oblak;GK;1993;SVN;86;86
Juan Musso;GK;1994;ARG;77;77
Cristian Romero;CB;1998;ARG;85;85
Dávid Hancko;CB;1997;SVK;80;81
Robin Le Normand;CB;1996;ESP;79;79
Marc Pubill;RB;2003;ESP;76;81
Álex Grimaldo;LB;1995;ESP;82;82
Morten Hjulmand;DM;1999;DEN;83;84
Johnny Cardoso;DM;2001;USA;79;82
Koke;CM;1992;ESP;77;77
Pablo Barrios;CM;2003;ESP;82;86
Marcos Llorente;CM;1995;ESP;81;81
Obed Vargas;CM;2005;MEX;71;80
Álex Baena;AM;2001;ESP;84;85
Lee Kang-in;AM;2001;KOR;81;82
Giuliano Simeone;RW;2002;ARG;80;83
Ademola Lookman;LW;1997;NGA;83;83
Julián Álvarez;ST;2000;ARG;88;89
Jonathan David;ST;2000;CAN;82;82
Alexander Sørloth;ST;1995;NOR;80;80
""",
        "Manchester City" to """
Gianluigi Donnarumma;GK;1999;ITA;88;88
Gerónimo Rulli;GK;1992;ARG;79;79
Marcus Bettinelli;GK;1992;ENG;68;68
Rúben Dias;CB;1997;POR;86;86
Joško Gvardiol;CB;2002;CRO;85;87
Marc Guéhi;CB;2000;ENG;84;85
Abdukodir Khusanov;CB;2004;UZB;79;85
Vitor Reis;CB;2006;BRA;72;84
Kaden Braithwaite;CB;2008;ENG;60;80
Rico Lewis;RB;2004;ENG;80;84
Matheus Nunes;RB;1998;POR;79;79
Rayan Aït-Nouri;LB;2001;ALG;82;83
Nico O'Reilly;LB;2005;ENG;79;85
Enzo Fernández;CM;2001;ARG;86;87
Elliot Anderson;CM;2002;ENG;83;86
Mateo Kovačić;CM;1994;CRO;79;79
Ayyoub Bouaddi;CM;2007;MAR;72;87
Phil Foden;AM;2000;ENG;85;86
Rayan Cherki;AM;2003;FRA;84;88
Floyd Samba;AM;2009;FRA;58;84
Jérémy Doku;LW;2002;BEL;83;85
Antoine Semenyo;LW;2000;GHA;83;84
Iliman Ndiaye;LW;2000;SEN;80;81
Allan;RW;2004;BRA;72;80
Ryan McAidoo;RW;2008;ENG;62;83
Erling Haaland;ST;2000;NOR;92;92
""",
        "Liverpool" to """
Alisson;GK;1992;BRA;86;86
Giorgi Mamardashvili;GK;2000;GEO;83;87
Freddie Woodman;GK;1997;ENG;68;68
Virgil van Dijk;CB;1991;NED;86;86
Joe Gomez;CB;1997;ENG;77;77
Giovanni Leoni;CB;2006;ITA;76;87
Jérémy Jacquet;CB;2005;FRA;76;85
Jeremie Frimpong;RB;2000;NED;82;83
Conor Bradley;RB;2003;NIR;79;83
Calvin Ramsay;RB;2003;SCO;69;74
Milos Kerkez;LB;2003;HUN;82;85
Kostas Tsimikas;LB;1996;GRE;76;76
Ryan Gravenberch;DM;2002;NED;85;87
Wataru Endo;DM;1993;JPN;75;75
Alexis Mac Allister;CM;1998;ARG;85;85
Curtis Jones;CM;2001;ENG;80;81
Trey Nyoni;CM;2007;ENG;70;86
Stefan Bajcetic;CM;2004;ESP;72;79
Florian Wirtz;AM;2003;GER;88;92
Dominik Szoboszlai;AM;2000;HUN;85;86
Harvey Elliott;AM;2003;ENG;78;82
Cody Gakpo;LW;1999;NED;83;83
Federico Chiesa;LW;1997;ITA;76;76
Rio Ngumoha;RW;2008;ENG;68;87
Alexander Isak;ST;1999;SWE;87;87
Hugo Ekitiké;ST;2002;FRA;84;87
Víctor Muñoz;ST;2003;ESP;74;80
""",
        "Arsenal" to """
David Raya;GK;1995;ESP;87;87
Kepa Arrizabalaga;GK;1994;ESP;77;77
Illan Meslier;GK;2000;FRA;76;78
William Saliba;CB;2001;FRA;88;89
Gabriel Magalhães;CB;1997;BRA;87;87
Piero Hincapié;CB;2002;ECU;82;85
Ezri Konsa;CB;1997;ENG;81;81
Cristhian Mosquera;CB;2004;ESP;78;84
Jurriën Timber;RB;2001;NED;84;85
Ben White;RB;1997;ENG;80;80
Riccardo Calafiori;LB;2002;ITA;81;84
Myles Lewis-Skelly;LB;2006;ENG;77;87
Martín Zubimendi;DM;1999;ESP;86;86
Bruno Guimarães;DM;1997;BRA;85;85
Declan Rice;CM;1999;ENG;88;88
Mikel Merino;CM;1996;ESP;81;81
Martin Ødegaard;AM;1998;NOR;85;85
Eberechi Eze;AM;1998;ENG;84;84
Bukayo Saka;RW;2001;ENG;88;90
Noni Madueke;RW;2002;ENG;81;84
Christos Tzolis;LW;2002;GRE;80;83
Viktor Gyökeres;ST;1998;SWE;85;85
Kai Havertz;ST;1999;GER;82;82
""",
        "Chelsea" to """
Emiliano Martínez;GK;1992;ARG;84;84
Mike Penders;GK;2005;BEL;71;84
Gabriel Slonina;GK;2004;USA;68;77
Reece James;RB;1999;ENG;83;83
Malo Gusto;RB;2003;FRA;80;83
Marco Palestra;RB;2005;ITA;76;85
Josh Acheampong;RB;2006;ENG;70;82
Levi Colwill;CB;2003;ENG;81;86
Wesley Fofana;CB;2000;FRA;79;80
Maxence Lacroix;CB;2000;FRA;81;82
Aarón Anselmino;CB;2005;ARG;72;82
Jorrel Hato;LB;2006;NED;78;87
Pep Chavarría;LB;1998;ESP;76;77
Moisés Caicedo;DM;2001;ECU;87;88
Roméo Lavia;DM;2004;BEL;79;84
Valentín Barco;CM;2004;ARG;76;81
Jordan Henderson;CM;1990;ENG;74;74
Reggie Watson;CM;2010;ENG;55;83
Cole Palmer;AM;2002;ENG;87;89
Morgan Rogers;AM;2002;ENG;84;87
Pedro Neto;RW;2000;POR;82;82
Estêvão;RW;2007;BRA;82;92
Geovany Quenda;RW;2007;POR;78;89
Jamie Gittens;LW;2004;ENG;78;84
João Pedro;ST;2001;BRA;84;85
Emmanuel Emegha;ST;2003;NED;76;81
Danny Welbeck;ST;1990;ENG;72;72
""",
        "Manchester United" to """
Senne Lammens;GK;2002;BEL;80;85
Tom Heaton;GK;1986;ENG;62;62
Diogo Dalot;RB;1999;POR;79;79
Noussair Mazraoui;RB;1997;MAR;78;78
Matthijs de Ligt;CB;1999;NED;81;82
Lisandro Martínez;CB;1998;ARG;81;81
Harry Maguire;CB;1993;ENG;77;77
Leny Yoro;CB;2005;FRA;80;87
Ayden Heaven;CB;2006;ENG;72;82
Harry Amass;LB;2007;ENG;72;84
Patrick Dorgu;LB;2004;DEN;78;83
Luke Shaw;LB;1995;ENG;76;76
Carlos Baleba;DM;2004;CMR;82;87
Manuel Ugarte;DM;2001;URU;78;79
Kobbie Mainoo;CM;2005;ENG;80;86
Youri Tielemans;CM;1997;BEL;80;80
Andrey Santos;CM;2004;BRA;79;84
Mason Mount;AM;1999;ENG;77;77
Bruno Fernandes;AM;1994;POR;85;85
Bryan Mbeumo;RW;1999;CMR;83;83
Amad Diallo;RW;2002;CIV;81;84
Marcus Rashford;LW;1997;ENG;79;79
Matheus Cunha;ST;1999;BRA;83;83
Benjamin Šeško;ST;2003;SVN;82;87
Joshua Zirkzee;ST;2001;NED;75;77
""",
        "Tottenham Hotspur" to """
Antonín Kinský;GK;2003;CZE;77;84
Martin Dúbravka;GK;1989;SVK;74;74
Pedro Porro;RB;1999;ESP;81;81
Micky van de Ven;CB;2001;NED;84;85
Jan Paul van Hecke;CB;2000;NED;80;81
Marcos Senesi;CB;1997;ARG;78;78
Tosin Adarabioyo;CB;1997;ENG;76;76
Ben Davies;CB;1993;WAL;74;74
Destiny Udogie;LB;2002;ITA;79;82
Andy Robertson;LB;1994;SCO;78;78
Rodrigo Bentancur;DM;1997;URU;78;78
Archie Gray;DM;2006;ENG;76;86
Sandro Tonali;CM;2000;ITA;84;85
Conor Gallagher;CM;2000;ENG;80;80
Lucas Bergvall;CM;2006;SWE;78;87
Mateus Fernandes;CM;2004;POR;77;83
Xavi Simons;AM;2003;NED;83;86
James Maddison;AM;1996;ENG;79;79
Mohammed Kudus;RW;2000;GHA;82;83
Dejan Kulusevski;RW;2000;SWE;81;81
Mathys Tel;LW;2005;FRA;77;85
Sávio;LW;2004;BRA;81;85
Mykhailo Mudryk;LW;2001;UKR;75;76
Wilson Odobert;LW;2004;FRA;75;81
Dominic Solanke;ST;1997;ENG;79;79
Omar Marmoush;ST;1999;EGY;81;81
Richarlison;ST;1997;BRA;76;76
""",
        "Newcastle United" to """
Nick Pope;GK;1992;ENG;80;80
Lukáš Horníček;GK;2002;CZE;72;79
Tino Livramento;RB;2002;ENG;81;84
Amar Dedić;RB;2002;BIH;78;82
Lewis Hall;LB;2004;ENG;80;85
Sven Botman;CB;2000;NED;80;82
Malick Thiaw;CB;2001;GER;80;82
Fabian Schär;CB;1991;SUI;76;76
Dan Burn;CB;1992;ENG;76;76
Nico González;DM;2002;ESP;79;83
Aladji Bamba;DM;2006;FRA;66;80
Joelinton;CM;1996;BRA;80;80
Joe Willock;CM;1999;ENG;76;76
Lewis Miley;CM;2006;ENG;76;84
Sean Steur;CM;2008;NED;62;82
Jacob Ramsey;AM;2001;ENG;77;79
Jacob Murphy;RW;1995;ENG;77;77
Anthony Elanga;RW;2002;SWE;80;82
Harvey Barnes;LW;1997;ENG;77;77
Bazoumana Touré;LW;2006;CIV;72;82
Yoane Wissa;ST;1996;COD;79;79
William Osula;ST;2003;DEN;75;80
""",
        "Bayern München" to """
Manuel Neuer;GK;1986;GER;83;83
Jonas Urbig;GK;2003;GER;77;84
Sven Ulreich;GK;1988;GER;67;67
Dayot Upamecano;CB;1998;FRA;84;84
Jonathan Tah;CB;1996;GER;84;84
Kim Min-jae;CB;1996;KOR;81;81
Hiroki Itō;CB;1999;JPN;78;79
Josip Stanišić;RB;2000;CRO;79;80
Sacha Boey;RB;2000;FRA;76;77
Alphonso Davies;LB;2000;CAN;81;82
Nathaniel Brown;LB;2003;GER;78;84
Joshua Kimmich;DM;1995;GER;87;87
Aleksandar Pavlović;CM;2004;GER;83;88
Konrad Laimer;CM;1997;AUT;80;80
Tom Bischof;CM;2005;GER;77;85
Jamal Musiala;AM;2003;GER;88;92
Lennart Karl;AM;2008;GER;77;90
Ismael Saibari;AM;2001;MAR;79;82
Michael Olise;RW;2001;FRA;88;90
Serge Gnabry;RW;1995;GER;78;78
Luis Díaz;LW;1997;COL;85;85
Harry Kane;ST;1993;ENG;90;90
""",
        "Borussia Dortmund" to """
Gregor Kobel;GK;1997;SUI;85;86
Alexander Meyer;GK;1991;GER;70;70
Nico Schlotterbeck;CB;1999;GER;84;85
Waldemar Anton;CB;1996;GER;80;80
Joane Gadou;CB;2007;FRA;72;84
Luca Reggiani;CB;2008;ITA;64;80
Julian Ryerson;RB;1997;NOR;78;78
Daniel Svensson;LB;2002;SWE;78;81
Ramy Bensebaini;LB;1995;ALG;75;75
Emre Can;DM;1994;GER;75;75
Felix Nmecha;CM;2000;GER;82;84
Jobe Bellingham;CM;2005;ENG;80;87
Marcel Sabitzer;CM;1994;AUT;77;77
Joey Veerman;CM;1998;NED;79;79
Carney Chukwuemeka;AM;2003;AUT;77;82
Ethan Nwaneri;AM;2007;ENG;79;89
Maximilian Beier;AM;2002;GER;80;83
Giannis Konstantelias;AM;2003;GRE;76;81
Konstantinos Karetsas;LW;2007;GRE;75;87
Serhou Guirassy;ST;1996;GUI;84;84
Fábio Silva;ST;2002;POR;78;81
""",
        "Paris Saint-Germain" to """
Lucas Chevalier;GK;2001;FRA;83;87
Matvey Safonov;GK;1999;RUS;79;80
Alessandro Longoni;GK;2008;ITA;58;80
Achraf Hakimi;RB;1998;MAR;87;87
Marquinhos;CB;1994;BRA;83;83
Willian Pacho;CB;2001;ECU;85;87
Illia Zabarnyi;CB;2002;UKR;82;85
Lucas Beraldo;CB;2003;BRA;78;82
Lucas Hernandez;LB;1996;FRA;79;79
Nuno Mendes;LB;2002;POR;87;88
Lucas Digne;LB;1993;FRA;75;75
Vitinha;DM;2000;POR;89;89
João Neves;CM;2004;POR;87;91
Warren Zaïre-Emery;CM;2006;FRA;81;87
Fabián Ruiz;CM;1996;ESP;82;82
Senny Mayulu;CM;2006;FRA;74;84
Dro Fernández;AM;2008;ESP;70;88
Maghnes Akliouche;RW;2002;FRA;81;84
Désiré Doué;RW;2005;FRA;86;92
Khvicha Kvaratskhelia;LW;2001;GEO;88;89
Mika Godts;LW;2005;BEL;77;85
Quentin Ndjantou;LW;2007;FRA;68;83
Ousmane Dembélé;ST;1997;FRA;90;90
Ferran Torres;ST;2000;ESP;80;80
""",
        "Inter" to """
Josep Martínez;GK;1998;ESP;80;82
Ivan Provedel;GK;1994;ITA;79;79
Alessandro Bastoni;CB;1999;ITA;86;86
Manuel Akanji;CB;1995;SUI;82;82
John Stones;CB;1994;ENG;79;79
Benjamin Pavard;CB;1996;FRA;79;79
Yann Bisseck;CB;2000;GER;80;83
Djed Spence;RB;2000;ENG;78;79
Federico Dimarco;LB;1997;ITA;83;83
Carlos Augusto;LB;1999;BRA;77;78
Hakan Çalhanoğlu;DM;1994;TUR;83;83
Aleksandar Stanković;DM;2005;SRB;75;84
Nicolò Barella;CM;1997;ITA;86;86
Curtis Jones;CM;2001;ENG;80;81
Piotr Zieliński;CM;1994;POL;78;78
Petar Sučić;CM;2003;CRO;79;84
Andy Diouf;CM;2003;FRA;76;80
Henrikh Mkhitaryan;AM;1989;ARM;76;76
Luis Henrique;RW;2001;BRA;78;80
Lautaro Martínez;ST;1997;ARG;87;87
Marcus Thuram;ST;1997;FRA;84;84
Ange-Yoan Bonny;ST;2003;CIV;78;83
Francesco Pio Esposito;ST;2005;ITA;77;87
""",
        "Milan" to """
Mike Maignan;GK;1995;FRA;85;85
Pietro Terracciano;GK;1990;ITA;70;70
Fikayo Tomori;CB;1997;ENG;79;79
Strahinja Pavlović;CB;2001;SRB;80;83
Koni De Winter;CB;2002;BEL;78;81
Mario Gila;CB;2000;ESP;81;82
Matteo Gabbia;CB;1999;ITA;77;77
Pervis Estupiñán;LB;1998;ECU;78;78
Davide Bartesaghi;LB;2005;ITA;74;83
Alexis Saelemaekers;RB;1999;BEL;78;78
Luka Modrić;DM;1985;CRO;81;81
Ardon Jashari;DM;2002;SUI;80;84
Adrien Rabiot;CM;1995;FRA;82;82
Ruben Loftus-Cheek;CM;1996;ENG;77;77
Yunus Musah;CM;2002;USA;77;80
Christian Comotto;CM;2008;ITA;62;81
Omari Hutchinson;AM;2003;ENG;79;84
Christian Pulisic;RW;1998;USA;84;84
Samuel Chukwueze;RW;1999;NGA;76;76
Diego Moreira;LW;2004;BEL;74;80
Gonçalo Ramos;ST;2001;POR;80;81
Francesco Camarda;ST;2008;ITA;70;88
""",
        "Juventus" to """
Guglielmo Vicario;GK;1996;ITA;82;82
Kamil Grabara;GK;1999;POL;79;81
Bremer;CB;1997;BRA;83;83
Pierre Kalulu;CB;2000;FRA;80;81
Federico Gatti;CB;1998;ITA;78;78
Lloyd Kelly;CB;1998;ENG;76;76
Jhon Lucumí;CB;1998;COL;80;80
Zeki Çelik;RB;1997;TUR;76;76
Andrea Cambiaso;LB;2000;ITA;82;83
Juan Cabal;LB;2001;COL;74;77
Manuel Locatelli;DM;1998;ITA;81;81
Khéphren Thuram;CM;2001;FRA;81;83
Teun Koopmeiners;CM;1998;NED;79;79
Weston McKennie;CM;1998;USA;78;78
Pape Matar Sarr;CM;2002;SEN;80;82
Douglas Luiz;CM;1998;BRA;77;77
Kenan Yıldız;AM;2005;TUR;86;92
Kerim Alajbegović;AM;2007;BIH;74;86
Nico González;AM;1998;ARG;78;78
Francisco Conceição;RW;2002;POR;81;84
Edon Zhegrova;RW;1999;KOS;80;80
Jérémie Boga;LW;1997;CIV;76;76
Nick Woltemade;ST;2002;GER;81;85
Randal Kolo Muani;ST;1998;FRA;79;79
Arkadiusz Milik;ST;1994;POL;74;74
Jeff Ekhator;ST;2006;ITA;70;83
""",
        "Napoli" to """
Alex Meret;GK;1997;ITA;80;80
Vanja Milinković-Savić;GK;1997;SRB;80;80
Giovanni Di Lorenzo;RB;1993;ITA;79;79
Rafa Marín;RB;2002;ESP;76;81
Amir Rrahmani;CB;1994;KOS;80;80
Alessandro Buongiorno;CB;1999;ITA;81;82
Sam Beukema;CB;1998;NED;80;80
Benoît Badiashile;CB;2001;FRA;77;78
Luca Marianucci;CB;2004;ITA;72;82
Mathías Olivera;LB;1997;URU;77;77
Leonardo Spinazzola;LB;1993;ITA;73;73
Stanislav Lobotka;DM;1994;SVK;82;82
Frank Anguissa;DM;1995;CMR;80;80
Scott McTominay;CM;1996;SCO;85;85
Billy Gilmour;CM;2001;SCO;79;80
Antonio Vergara;CM;2003;ITA;74;80
Kevin De Bruyne;AM;1991;BEL;83;83
Matteo Politano;RW;1993;ITA;77;77
David Neres;LW;1997;BRA;80;80
Noa Lang;LW;1999;NED;80;80
Giovane;LW;2003;BRA;74;80
Rasmus Højlund;ST;2003;DEN;82;86
Lorenzo Lucca;ST;2000;ITA;77;78
""",
        "Benfica" to """
Anatoliy Trubin;GK;2001;UKR;83;86
Samuel Soares;GK;2002;POR;72;78
Tomás Araújo;CB;2002;POR;80;84
Clément Lenglet;CB;1995;FRA;77;77
Alessandro Circati;CB;2003;AUS;74;79
Daniel Banjaqui;CB;2006;POR;65;80
Samuel Dahl;LB;2003;SWE;76;80
Souffian El Karouani;LB;2000;MAR;76;78
Alexander Bah;RB;1997;DEN;77;77
João Palhinha;DM;1995;POR;80;80
Enzo Barrenechea;DM;2001;ARG;78;81
Fredrik Aursnes;CM;1995;NOR;79;79
Leandro Barreiro;CM;2000;LUX;77;78
Manu Silva;CM;2001;POR;75;78
Heorhiy Sudakov;AM;2002;UKR;81;84
Rafa Silva;AM;1993;POR;79;79
Claudio Echeverri;AM;2006;ARG;74;86
Dodi Lukébakio;RW;1997;BEL;80;80
Andreas Schjelderup;RW;2004;NOR;76;83
Gianluca Prestianni;RW;2006;ARG;74;85
Jakub Kamiński;LW;2002;POL;77;79
Vangelis Pavlidis;ST;1998;GRE;82;82
Jhon Durán;ST;2003;COL;79;84
Anísio Cabral;ST;2008;POR;64;82
"""
    )
}
