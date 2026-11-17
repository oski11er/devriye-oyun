package com.futbolmenajer.data.db

/**
 * LİG KATALOĞU
 * ---------------------------------------------------------------
 * 50 ülke, 85+ lig. Büyük liglerin üst kümeleri 2025-26 sezonu kulüp
 * listeleriyle gelir; diğer ligler ülkenin şehir listesinden üretilir.
 *
 * İstersen tüm bunları assets/database/leagues.json ile tamamen
 * değiştirebilirsin (DatabaseImporter bkz.). JSON yoksa bu katalog kullanılır.
 */
data class LeagueDef(
    val name: String, val size: Int, val promo: Int = 3, val releg: Int = 3,
    val clubs: List<String> = emptyList(), val playoff: Boolean = false
)

data class CountryDef(
    val code: String,             // ISO-3
    val nameTr: String,
    val uefa: Boolean,
    val coefficient: Double,      // lig gücü / UEFA ülke puanı benzeri
    val nameGroup: NameGroup,
    val leagues: List<LeagueDef>,
    val cupName: String,
    val cities: List<String> = emptyList()
)

object LeagueCatalog {

    val countries: List<CountryDef> = listOf(
        CountryDef("ENG", "İngiltere", true, 98.0, NameGroup.ENGLISH, listOf(
            LeagueDef("Premier League", 20, 0, 3, listOf("Arsenal", "Aston Villa", "Bournemouth", "Brentford", "Brighton", "Chelsea", "Coventry City", "Crystal Palace", "Everton", "Fulham", "Hull City", "Ipswich Town", "Leeds United", "Liverpool", "Manchester City", "Manchester United", "Newcastle United", "Nottingham Forest", "Sunderland", "Tottenham Hotspur")),
            LeagueDef("Championship", 24, 3, 3, listOf("Birmingham City", "Blackburn Rovers", "Bristol City", "Burnley", "Charlton Athletic", "Derby County", "West Ham United", "Wolverhampton", "Leicester City", "Middlesbrough", "Millwall", "Norwich City", "Oxford United", "Portsmouth", "Preston North End", "QPR", "Sheffield United", "Sheffield Wednesday", "Southampton", "Stoke City", "Swansea City", "Watford", "West Bromwich", "Wrexham"), playoff = true),
            LeagueDef("League One", 24, 3, 4, playoff = true),
            LeagueDef("League Two", 24, 4, 2, playoff = true),
            LeagueDef("National League", 24, 2, 4)
        ), "FA Cup", listOf("Reading", "Bolton", "Wigan", "Barnsley", "Rotherham", "Huddersfield", "Plymouth", "Luton", "Cardiff", "Blackpool", "Peterborough", "Lincoln", "Exeter", "Cambridge", "Northampton", "Stevenage", "Wycombe", "Burton", "Mansfield", "Leyton", "Bradford", "Doncaster", "Gillingham", "Crewe", "Walsall", "Port Vale", "Notts", "Tranmere", "Salford", "Swindon", "Colchester", "Grimsby", "Carlisle", "Chesterfield", "Bromley", "Barrow", "Accrington", "Newport", "Fleetwood", "Harrogate", "Morecambe", "Shrewsbury", "Cheltenham", "Bristol", "Crawley", "AFC Wimbledon", "Milton Keynes", "Barnet", "York", "Oldham", "Southend", "Hartlepool", "Halifax", "Rochdale", "Aldershot", "Woking", "Eastleigh", "Boreham", "Dagenham", "Yeovil", "Altrincham", "Solihull", "Gateshead", "Wealdstone", "Maidenhead", "Ebbsfleet", "Tamworth", "Sutton", "Braintree", "Forest Green", "Hereford", "Kidderminster", "Scunthorpe", "Torquay", "Weymouth", "Dorking", "Chester")),

        CountryDef("ESP", "İspanya", true, 92.0, NameGroup.SPANISH, listOf(
            LeagueDef("LaLiga", 20, 0, 3, listOf("Alavés", "Athletic Club", "Atlético Madrid", "Barcelona", "Celta Vigo", "Deportivo", "Elche", "Espanyol", "Getafe", "Levante", "Málaga", "Osasuna", "Racing Santander", "Rayo Vallecano", "Real Betis", "Real Madrid", "Real Sociedad", "Sevilla", "Valencia", "Villarreal")),
            LeagueDef("LaLiga Hypermotion", 22, 3, 4, playoff = true),
            LeagueDef("Primera Federación", 20, 4, 5)
        ), "Copa del Rey", listOf("Mallorca", "Girona", "Real Oviedo", "Zaragoza", "Sporting Gijón", "Granada", "Almería", "Las Palmas", "Valladolid", "Leganés", "Cádiz", "Eibar", "Huesca", "Tenerife", "Albacete", "Burgos", "Mirandés", "Castellón", "Córdoba", "Cartagena", "Ferrol", "Eldense", "Andorra", "Ceuta", "Lugo", "Ponferradina", "Murcia", "Hércules", "Alcorcón", "Badajoz", "Salamanca", "Logroñés", "Gimnàstic", "Sabadell", "Cultural Leonesa", "Algeciras", "Mérida", "Recreativo", "Lorca", "Ibiza", "Teruel", "Osasuna B", "Sevilla Atlético", "Bilbao Athletic", "Real Unión", "Arenas", "Fuenlabrada", "Talavera", "Antequera", "Marbella")),

        CountryDef("GER", "Almanya", true, 85.0, NameGroup.GERMAN, listOf(
            LeagueDef("Bundesliga", 18, 0, 2, listOf("Bayern München", "Borussia Dortmund", "Bayer Leverkusen", "RB Leipzig", "Eintracht Frankfurt", "VfB Stuttgart", "SC Freiburg", "FC Schalke 04", "Borussia Mönchengladbach", "1. FSV Mainz 05", "TSG Hoffenheim", "Werder Bremen", "FC Augsburg", "1. FC Union Berlin", "SV Elversberg", "SC Paderborn 07", "1. FC Köln", "Hamburger SV"), playoff = true),
            LeagueDef("2. Bundesliga", 18, 2, 2, playoff = true),
            LeagueDef("3. Liga", 20, 2, 4)
        ), "DFB-Pokal", listOf("VfL Wolfsburg", "FC St. Pauli", "1. FC Heidenheim", "Hertha", "Hannover", "Nürnberg", "Kaiserslautern", "Düsseldorf", "Karlsruhe", "Darmstadt", "Bochum", "Kiel", "Magdeburg", "Braunschweig", "Münster", "Fürth", "Regensburg", "Ulm", "Bielefeld", "Dresden", "Rostock", "Saarbrücken", "Aachen", "Osnabrück", "Duisburg", "Essen", "Wiesbaden", "Mannheim", "Ingolstadt", "Verl", "Cottbus", "Sandhausen", "Aue", "Unterhaching", "Halle", "Lübeck", "Oldenburg", "Offenbach", "Jena", "Chemnitz")),

        CountryDef("ITA", "İtalya", true, 88.0, NameGroup.ITALIAN, listOf(
            LeagueDef("Serie A", 20, 0, 3, listOf("Atalanta", "Bologna", "Cagliari", "Como", "Fiorentina", "Frosinone", "Genoa", "Inter", "Juventus", "Lazio", "Lecce", "Milan", "Monza", "Napoli", "Parma", "Roma", "Sassuolo", "Torino", "Udinese", "Venezia")),
            LeagueDef("Serie B", 20, 3, 4, playoff = true),
            LeagueDef("Serie C", 20, 4, 4)
        ), "Coppa Italia", listOf("Cremonese", "Hellas Verona", "Pisa", "Sampdoria", "Palermo", "Bari", "Empoli", "Spezia", "Salernitana", "Brescia", "Reggiana", "Modena", "Catanzaro", "Cesena", "Südtirol", "Cosenza", "Mantova", "Carrarese", "Juve Stabia", "Padova", "Avellino", "Pescara", "Virtus Entella", "Ternana", "Vicenza", "Triestina", "Perugia", "Ascoli", "Catania", "Foggia", "Novara", "Alessandria", "Lucchese", "Arezzo", "Rimini", "Pro Vercelli", "Taranto", "Benevento", "Crotone", "Messina", "Livorno", "Siena")),

        CountryDef("FRA", "Fransa", true, 72.0, NameGroup.FRENCH, listOf(
            LeagueDef("Ligue 1", 18, 0, 2, listOf("Angers", "Auxerre", "Brest", "Le Havre", "Le Mans", "Lens", "Lille", "Lorient", "Lyon", "Marseille", "Monaco", "Nice", "Paris FC", "Paris Saint-Germain", "Rennes", "Strasbourg", "Toulouse", "Troyes"), playoff = true),
            LeagueDef("Ligue 2", 18, 2, 3, playoff = true),
            LeagueDef("National", 18, 2, 4)
        ), "Coupe de France", listOf("Metz", "Nantes", "Saint-Étienne", "Montpellier", "Reims", "Bordeaux", "Guingamp", "Caen", "Amiens", "Grenoble", "Clermont", "Annecy", "Ajaccio", "Bastia", "Pau", "Laval", "Rodez", "Dunkerque", "Red Star", "Nancy", "Sochaux", "Valenciennes", "Dijon", "Quevilly", "Niort", "Châteauroux", "Orléans", "Nîmes", "Boulogne", "Versailles", "Martigues", "Concarneau", "Avranches", "Villefranche", "Bourg")),

        CountryDef("POR", "Portekiz", true, 64.0, NameGroup.PORTUGUESE, listOf(
            LeagueDef("Liga Portugal", 18, 0, 2, listOf("Benfica", "Porto", "Sporting CP", "Braga", "Vitória SC", "Famalicão", "Gil Vicente", "Moreirense", "Rio Ave", "Arouca", "Estoril", "Casa Pia", "Santa Clara", "Nacional", "Estrela da Amadora", "AVS", "Tondela", "Alverca"), playoff = true),
            LeagueDef("Liga Portugal 2", 18, 2, 2)
        ), "Taça de Portugal", listOf("Boavista", "Farense", "Marítimo", "Académico Viseu", "Chaves", "Feirense", "Penafiel", "Leixões", "Torreense", "Paços de Ferreira", "Oliveirense", "Portimonense", "Vizela", "Felgueiras", "Belenenses", "Académica", "Benfica B", "Porto B", "Mafra", "Varzim")),

        CountryDef("NED", "Hollanda", true, 60.0, NameGroup.DUTCH, listOf(
            LeagueDef("Eredivisie", 18, 0, 2, listOf("Ajax", "PSV", "Feyenoord", "AZ", "FC Twente", "FC Utrecht", "NEC", "Go Ahead Eagles", "sc Heerenveen", "Sparta Rotterdam", "FC Groningen", "Fortuna Sittard", "PEC Zwolle", "Heracles Almelo", "NAC Breda", "Excelsior", "FC Volendam", "Telstar"), playoff = true),
            LeagueDef("Keuken Kampioen Divisie", 20, 2, 0, playoff = true)
        ), "KNVB Beker", listOf("Willem II", "Vitesse", "RKC Waalwijk", "Almere City", "Roda JC", "ADO Den Haag", "De Graafschap", "FC Emmen", "Cambuur", "Dordrecht", "MVV", "VVV-Venlo", "Helmond Sport", "FC Den Bosch", "TOP Oss", "FC Eindhoven", "Jong Ajax", "Jong PSV", "Jong AZ", "Jong Utrecht")),

        CountryDef("TUR", "Türkiye", true, 40.0, NameGroup.TURKISH, listOf(
            LeagueDef("Trendyol Süper Lig", 18, 0, 4, listOf("Galatasaray", "Fenerbahçe", "Beşiktaş", "Trabzonspor", "Başakşehir", "Samsunspor", "Göztepe", "Eyüpspor", "Kasımpaşa", "Kocaelispor", "Gençlerbirliği", "Alanyaspor", "Konyaspor", "Gaziantep FK", "Çaykur Rizespor", "Amedspor", "Çorum FK", "Erzurumspor")),
            LeagueDef("Trendyol 1. Lig", 20, 3, 4, listOf("Antalyaspor", "Bandırmaspor", "Bodrum FK", "Boluspor", "Kayserispor", "Esenler Erokspor", "Hatayspor", "Iğdır FK", "İstanbulspor", "Keçiörengücü", "Manisa FK", "Pendikspor", "Sakaryaspor", "Sarıyer", "Serik Belediyespor", "Sivasspor", "Ümraniyespor", "Vanspor", "Adana Demirspor", "Fatih Karagümrük"), playoff = true),
            LeagueDef("Nesine 2. Lig", 20, 3, 4, playoff = true),
            LeagueDef("Nesine 3. Lig", 20, 3, 0, playoff = true)
        ), "Ziraat Türkiye Kupası", listOf("Ankaragücü", "Bursaspor", "Altay", "Adanaspor", "Giresunspor", "Denizlispor", "Eskişehirspor", "Malatyaspor", "Kardemir Karabükspor", "Tuzlaspor", "Altınordu", "Menemen", "Şanlıurfaspor", "Kırklarelispor", "Tarsus İdman Yurdu", "Afyonspor", "Uşakspor", "Elazığspor", "Diyarbekirspor", "Bucaspor", "Karşıyaka", "Zonguldakspor", "Orduspor", "Balıkesirspor", "Mardinspor", "Batman Petrolspor", "Muğlaspor", "Yeni Mersin İY", "Isparta 32", "Kastamonuspor", "Düzcespor", "Yalovaspor", "Edirnespor", "Nazilli Belediyespor", "Fethiyespor", "Somaspor", "Bayburt ÖİS", "Kahramanmaraşspor", "Siirt İÖİ", "Artvin Hopaspor", "Sincan Belediyespor", "Ağrı 1970", "Kırşehirspor", "Aksarayspor", "Niğde Anadolu", "Çankaya FK", "Bergama Belediyespor", "Ayvalıkgücü", "Bornova 1877", "Kuşadasıspor", "Arnavutköy Belediye", "Beykoz Anadolu", "Beyoğlu Yeni Çarşı", "Tokat Belediye", "Amasyaspor", "Sivas Dört Eylül", "Erciyes 38", "Nevşehir Belediye", "Osmaniyespor", "Kilis Belediye")),

        CountryDef("BEL", "Belçika", true, 55.0, NameGroup.DUTCH, listOf(
            LeagueDef("Jupiler Pro League", 16, 0, 2, listOf("Club Brugge", "Anderlecht", "Union SG", "Genk", "Gent", "Antwerp", "Standard Liège", "Charleroi", "Mechelen", "Westerlo", "Cercle Brugge", "OH Leuven", "Sint-Truiden", "Dender", "Zulte Waregem", "La Louvière")),
            LeagueDef("Challenger Pro League", 16, 2, 2)
        ), "Croky Cup", listOf("Kortrijk", "Beerschot", "Eupen", "Lommel", "Lierse", "Patro Eisden", "Seraing", "RWDM", "Deinze", "Beveren", "Francs Borains", "Club NXT", "Jong Genk", "RFC Liège", "Olympic Charleroi", "Mouscron")),

        CountryDef("SCO", "İskoçya", true, 33.0, NameGroup.ENGLISH, listOf(
            LeagueDef("Scottish Premiership", 12, 0, 1, listOf("Celtic", "Rangers", "Hearts", "Hibernian", "Aberdeen", "Motherwell", "Dundee United", "Dundee", "Kilmarnock", "St Mirren", "Livingston", "Falkirk"), playoff = true),
            LeagueDef("Scottish Championship", 10, 1, 1, playoff = true)
        ), "Scottish Cup", listOf("St Johnstone", "Ross County", "Partick Thistle", "Raith Rovers", "Ayr United", "Queen's Park", "Greenock Morton", "Airdrieonians", "Arbroath", "Hamilton", "Dunfermline")),

        CountryDef("AUT", "Avusturya", true, 30.0, NameGroup.GERMAN, listOf(LeagueDef("Bundesliga (AUT)", 12, 0, 1), LeagueDef("2. Liga (AUT)", 16, 1, 2)), "ÖFB-Cup",
            listOf("Salzburg", "Rapid Wien", "Austria Wien", "Sturm Graz", "LASK", "Wolfsberg", "Hartberg", "Altach", "Ried", "BW Linz", "WSG Tirol", "Grazer AK", "Admira", "Austria Klagenfurt", "Lustenau", "St. Pölten", "Kapfenberg", "Amstetten", "Horn", "Liefering", "Bregenz", "Floridsdorf", "Wacker Innsbruck", "Leoben", "Vienna", "Stripfing", "Sturm II", "Rapid II")),
        CountryDef("SUI", "İsviçre", true, 31.0, NameGroup.GERMAN, listOf(LeagueDef("Super League", 12, 0, 1), LeagueDef("Challenge League", 10, 1, 1)), "Schweizer Cup",
            listOf("Young Boys", "Basel", "Servette", "Lugano", "Zürich", "Grasshoppers", "St. Gallen", "Luzern", "Lausanne", "Sion", "Winterthur", "Thun", "Aarau", "Vaduz", "Xamax", "Yverdon", "Wil", "Bellinzona", "Schaffhausen", "Stade Nyonnais", "Étoile Carouge", "Baden")),
        CountryDef("GRE", "Yunanistan", true, 30.0, NameGroup.GREEK, listOf(LeagueDef("Super League (GRE)", 14, 0, 2), LeagueDef("Super League 2", 16, 2, 4)), "Yunanistan Kupası",
            listOf("Olympiakos", "Panathinaikos", "AEK Atina", "PAOK", "Aris", "OFI", "Asteras Tripolis", "Atromitos", "Panetolikos", "Volos", "Levadiakos", "Larissa", "Kifisia", "Panserraikos", "Lamia", "Kalamata", "Iraklis", "Kallithea", "Chania", "Niki Volou", "Ionikos", "Panachaiki", "Egaleo", "Kavala", "Marko", "Ilioupoli", "Makedonikos", "Athens Kallithea", "Apollon Smyrnis", "Veria")),
        CountryDef("CZE", "Çekya", true, 32.0, NameGroup.SLAVIC_WEST, listOf(LeagueDef("Chance Liga", 16, 0, 2)), "MOL Cup",
            listOf("Slavia Praha", "Sparta Praha", "Viktoria Plzeň", "Baník Ostrava", "Slovan Liberec", "Mladá Boleslav", "Sigma Olomouc", "Hradec Králové", "Bohemians 1905", "Jablonec", "Teplice", "Pardubice", "Karviná", "Slovácko", "Zlín", "Dukla Praha")),
        CountryDef("DEN", "Danimarka", true, 28.0, NameGroup.NORDIC, listOf(LeagueDef("Superliga", 12, 0, 2), LeagueDef("1. Division (DEN)", 12, 2, 2)), "DBU Pokalen",
            listOf("København", "Midtjylland", "Brøndby", "Nordsjælland", "AGF", "Silkeborg", "Randers", "Viborg", "OB", "Vejle", "Sønderjyske", "Fredericia", "Lyngby", "AaB", "Horsens", "Esbjerg", "Hvidovre", "Kolding", "HB Køge", "Hillerød", "B.93", "Roskilde", "Hobro", "Vendsyssel")),
        CountryDef("NOR", "Norveç", true, 29.0, NameGroup.NORDIC, listOf(LeagueDef("Eliteserien", 16, 0, 2)), "NM Cupen",
            listOf("Bodø/Glimt", "Brann", "Molde", "Rosenborg", "Viking", "Vålerenga", "Sarpsborg", "Fredrikstad", "Tromsø", "Strømsgodset", "KFUM Oslo", "HamKam", "Sandefjord", "Kristiansund", "Bryne", "Haugesund")),
        CountryDef("SWE", "İsveç", true, 22.0, NameGroup.NORDIC, listOf(LeagueDef("Allsvenskan", 16, 0, 2)), "Svenska Cupen",
            listOf("Malmö FF", "Mjällby", "Hammarby", "AIK", "Djurgården", "IFK Göteborg", "Häcken", "Elfsborg", "IFK Norrköping", "Sirius", "Halmstad", "GAIS", "Brommapojkarna", "Degerfors", "Värnamo", "Öster")),
        CountryDef("CRO", "Hırvatistan", true, 27.0, NameGroup.SLAVIC_SOUTH, listOf(LeagueDef("HNL", 10, 0, 1)), "Hırvatistan Kupası",
            listOf("Dinamo Zagreb", "Hajduk Split", "Rijeka", "Osijek", "Varaždin", "Lokomotiva", "Slaven Belupo", "Istra 1961", "Gorica", "Vukovar")),
        CountryDef("SRB", "Sırbistan", true, 26.0, NameGroup.SLAVIC_SOUTH, listOf(LeagueDef("Super Liga (SRB)", 16, 0, 2)), "Sırbistan Kupası",
            listOf("Crvena Zvezda", "Partizan", "TSC", "Čukarički", "Vojvodina", "Radnički Niš", "Novi Pazar", "OFK Beograd", "Spartak Subotica", "Mladost", "IMT", "Železničar", "Radnik", "Napredak", "Javor", "Jedinstvo")),
        CountryDef("POL", "Polonya", true, 29.0, NameGroup.SLAVIC_WEST, listOf(LeagueDef("Ekstraklasa", 18, 0, 3), LeagueDef("I Liga", 18, 3, 3)), "Puchar Polski",
            listOf("Lech Poznań", "Legia Warszawa", "Raków", "Jagiellonia", "Pogoń Szczecin", "Górnik Zabrze", "Cracovia", "GKS Katowice", "Widzew Łódź", "Motor Lublin", "Zagłębie Lubin", "Korona Kielce", "Piast Gliwice", "Lechia Gdańsk", "Radomiak", "Arka Gdynia", "Bruk-Bet", "Wisła Płock", "Wisła Kraków", "Śląsk Wrocław", "Ruch Chorzów", "Polonia Warszawa", "Odra Opole", "Stal Mielec", "Puszcza", "Miedź Legnica", "Górnik Łęczna", "Chrobry", "ŁKS Łódź", "Warta Poznań", "Znicz", "Stal Rzeszów", "Pogoń Siedlce", "Wieczysta", "Polonia Bytom", "Kotwica")),
        CountryDef("UKR", "Ukrayna", true, 24.0, NameGroup.SLAVIC_EAST, listOf(LeagueDef("Premier Liha", 16, 0, 2)), "Ukrayna Kupası",
            listOf("Shakhtar Donetsk", "Dynamo Kyiv", "Polissya", "Oleksandriya", "LNZ Cherkasy", "Kryvbas", "Karpaty Lviv", "Zorya Luhansk", "Kolos Kovalivka", "Veres Rivne", "Rukh Lviv", "Obolon", "Metalist 1925", "Epitsentr", "Kudrivka", "Poltava")),
        CountryDef("ROU", "Romanya", true, 18.0, NameGroup.ROMANIAN, listOf(LeagueDef("SuperLiga (ROU)", 16, 0, 2)), "Cupa României",
            listOf("FCSB", "CFR Cluj", "Universitatea Craiova", "Rapid București", "Dinamo București", "Universitatea Cluj", "Farul Constanța", "Petrolul", "Oțelul Galați", "Botoșani", "Hermannstadt", "UTA Arad", "Csíkszereda", "Metaloglobus", "Unirea Slobozia", "Argeș")),
        CountryDef("HUN", "Macaristan", true, 17.0, NameGroup.HUNGARIAN, listOf(LeagueDef("NB I", 12, 0, 2)), "Magyar Kupa",
            listOf("Ferencváros", "Puskás Akadémia", "Paks", "Győr", "Debrecen", "MTK", "Újpest", "Zalaegerszeg", "Diósgyőr", "Kisvárda", "Nyíregyháza", "Kazincbarcika")),
        CountryDef("ISR", "İsrail", true, 22.0, NameGroup.HEBREW, listOf(LeagueDef("Ligat ha'Al", 14, 0, 2)), "İsrail Kupası",
            listOf("Maccabi Tel Aviv", "Hapoel Be'er Sheva", "Maccabi Haifa", "Beitar Jerusalem", "Hapoel Tel Aviv", "Maccabi Netanya", "Hapoel Haifa", "Bnei Sakhnin", "Hapoel Jerusalem", "Ironi Kiryat Shmona", "Ashdod", "Maccabi Bnei Reineh", "Ironi Tiberias", "Hapoel Petah Tikva")),
        CountryDef("CYP", "Kıbrıs", true, 21.0, NameGroup.GREEK, listOf(LeagueDef("Cyprus League", 14, 0, 3)), "Kıbrıs Kupası",
            listOf("Pafos FC", "APOEL", "Omonia", "AEK Larnaca", "Aris Limassol", "Apollon Limassol", "Anorthosis", "AEL Limassol", "Ethnikos Achna", "Omonia Aradippou", "Krasava", "Enosis Paralimni", "Akritas Chlorakas", "Olympiakos Nicosia")),
        CountryDef("AZE", "Azerbaycan", true, 14.0, NameGroup.TURKISH, listOf(LeagueDef("Premyer Liqa", 12, 0, 1)), "Azerbaycan Kupası",
            listOf("Qarabağ", "Sabah", "Neftçi", "Zirə", "Turan Tovuz", "Sumqayıt", "Araz-Naxçıvan", "Kəpəz", "Şamaxı", "Qəbələ", "İmişli", "Karvan")),
        CountryDef("SVK", "Slovakya", true, 16.0, NameGroup.SLAVIC_WEST, listOf(LeagueDef("Niké Liga", 12, 0, 1)), "Slovnaft Cup",
            listOf("Slovan Bratislava", "DAC Dunajská Streda", "Spartak Trnava", "Žilina", "Podbrezová", "Ružomberok", "Michalovce", "Trenčín", "Komárno", "Skalica", "Prešov", "Košice")),
        CountryDef("SVN", "Slovenya", true, 14.0, NameGroup.SLAVIC_SOUTH, listOf(LeagueDef("PrvaLiga", 10, 0, 1)), "Slovenya Kupası",
            listOf("Olimpija Ljubljana", "Celje", "Maribor", "Koper", "Bravo", "Mura", "Radomlje", "Domžale", "Primorje", "Aluminij")),
        CountryDef("BUL", "Bulgaristan", true, 12.0, NameGroup.SLAVIC_EAST, listOf(LeagueDef("Parva Liga", 16, 0, 2)), "Bulgaristan Kupası",
            listOf("Ludogorets", "Levski Sofia", "CSKA Sofia", "Cherno More", "Lokomotiv Plovdiv", "Botev Plovdiv", "Arda", "Slavia Sofia", "CSKA 1948", "Beroe", "Spartak Varna", "Lokomotiv Sofia", "Septemvri", "Botev Vratsa", "Dobrudzha", "Montana")),
        CountryDef("MKD", "Kuzey Makedonya", true, 6.0, NameGroup.SLAVIC_SOUTH, listOf(LeagueDef("Prva MFL", 12, 0, 2)), "Makedonya Kupası",
            listOf("Shkëndija", "Vardar", "Struga", "Shkupi", "Rabotnički", "Tikvesh", "Bashkimi", "Sileks", "Pelister", "Arsimi", "Voska Sport", "Makedonija GP")),
        CountryDef("IRL", "İrlanda", true, 9.0, NameGroup.ENGLISH, listOf(LeagueDef("League of Ireland", 10, 0, 1)), "FAI Cup",
            listOf("Shamrock Rovers", "Shelbourne", "Bohemians", "Derry City", "St Patrick's Athletic", "Drogheda United", "Sligo Rovers", "Galway United", "Waterford", "Cork City")),
        CountryDef("FIN", "Finlandiya", true, 8.0, NameGroup.NORDIC, listOf(LeagueDef("Veikkausliiga", 12, 0, 1)), "Suomen Cup",
            listOf("KuPS", "HJK", "Ilves", "SJK", "Inter Turku", "Gnistan", "VPS", "FC Lahti", "Haka", "AC Oulu", "IFK Mariehamn", "KTP")),
        CountryDef("KAZ", "Kazakistan", true, 10.0, NameGroup.SLAVIC_EAST, listOf(LeagueDef("Premier League (KAZ)", 14, 0, 2)), "Kazakistan Kupası",
            listOf("Kairat", "Astana", "Tobol", "Aktobe", "Ordabasy", "Elimai", "Atyrau", "Kaisar", "Zhenis", "Okzhetpes", "Kyzylzhar", "Zhetysu", "Turan", "Ulytau")),
        CountryDef("RUS", "Rusya (UEFA dışı – men cezası)", false, 20.0, NameGroup.SLAVIC_EAST, listOf(LeagueDef("Premier Liga (RUS)", 16, 0, 2)), "Rusya Kupası",
            listOf("Zenit", "Krasnodar", "Spartak Moskova", "Dinamo Moskova", "CSKA Moskova", "Lokomotiv Moskova", "Rubin Kazan", "Rostov", "Baltika", "Akhmat Grozny", "Krylia Sovetov", "Dinamo Makhachkala", "Orenburg", "Akron Tolyatti", "Pari NN", "Sochi")),

        // ---------------- UEFA dışı ----------------
        CountryDef("BRA", "Brezilya", false, 60.0, NameGroup.PORTUGUESE, listOf(LeagueDef("Brasileirão Série A", 20, 0, 4), LeagueDef("Brasileirão Série B", 20, 4, 4)), "Copa do Brasil",
            listOf("Flamengo", "Palmeiras", "São Paulo", "Corinthians", "Santos", "Grêmio", "Internacional", "Atlético Mineiro", "Cruzeiro", "Fluminense", "Botafogo", "Vasco da Gama", "Bahia", "Fortaleza", "Red Bull Bragantino", "Vitória", "Juventude", "Sport Recife", "Ceará", "Mirassol", "Athletico Paranaense", "Coritiba", "Goiás", "Chapecoense", "Remo", "Criciúma", "Avaí", "Ponte Preta", "Guarani", "Novorizontino", "Cuiabá", "América Mineiro", "Paysandu", "Operário", "CRB", "Atlético Goianiense", "Vila Nova", "Botafogo-SP", "Amazonas", "Ferroviária")),
        CountryDef("ARG", "Arjantin", false, 50.0, NameGroup.SPANISH, listOf(LeagueDef("Liga Profesional", 28, 0, 2)), "Copa Argentina",
            listOf("River Plate", "Boca Juniors", "Racing Club", "Independiente", "San Lorenzo", "Estudiantes", "Vélez Sarsfield", "Talleres", "Rosario Central", "Newell's Old Boys", "Lanús", "Huracán", "Argentinos Juniors", "Defensa y Justicia", "Belgrano", "Godoy Cruz", "Platense", "Tigre", "Banfield", "Gimnasia LP", "Unión", "Instituto", "Atlético Tucumán", "Barracas Central", "Central Córdoba", "Independiente Rivadavia", "Sarmiento", "Aldosivi")),
        CountryDef("USA", "ABD / Kanada (MLS)", false, 35.0, NameGroup.ENGLISH, listOf(LeagueDef("Major League Soccer", 30, 0, 0)), "US Open Cup",
            listOf("Inter Miami", "LA Galaxy", "LAFC", "Seattle Sounders", "Atlanta United", "New York City FC", "New York Red Bulls", "Columbus Crew", "FC Cincinnati", "Philadelphia Union", "Orlando City", "Nashville SC", "Charlotte FC", "Toronto FC", "CF Montréal", "Vancouver Whitecaps", "Portland Timbers", "Real Salt Lake", "Sporting Kansas City", "Minnesota United", "Chicago Fire", "DC United", "New England Revolution", "Houston Dynamo", "FC Dallas", "Austin FC", "Colorado Rapids", "San Jose Earthquakes", "St. Louis City", "San Diego FC")),
        CountryDef("MEX", "Meksika", false, 38.0, NameGroup.SPANISH, listOf(LeagueDef("Liga MX", 18, 0, 0)), "Copa MX",
            listOf("América", "Guadalajara", "Cruz Azul", "UNAM Pumas", "Monterrey", "Tigres UANL", "Toluca", "Pachuca", "León", "Santos Laguna", "Atlas", "Necaxa", "Puebla", "Querétaro", "Juárez", "Tijuana", "Mazatlán", "Atlético San Luis")),
        CountryDef("KSA", "Suudi Arabistan", false, 40.0, NameGroup.ARABIC, listOf(LeagueDef("Saudi Pro League", 18, 0, 3)), "King's Cup",
            listOf("Al-Hilal", "Al-Nassr", "Al-Ittihad", "Al-Ahli", "Al-Qadsiah", "Al-Shabab", "Al-Ettifaq", "Al-Taawoun", "Al-Fateh", "Al-Khaleej", "Al-Fayha", "Al-Riyadh", "Damac", "Al-Kholood", "Al-Okhdood", "Al-Hazem", "Al-Najma", "NEOM")),
        CountryDef("JPN", "Japonya", false, 30.0, NameGroup.JAPANESE, listOf(LeagueDef("J1 League", 20, 0, 3), LeagueDef("J2 League", 20, 3, 3)), "Emperor's Cup",
            listOf("Vissel Kobe", "Sanfrecce Hiroshima", "Kashima Antlers", "Kashiwa Reysol", "Urawa Reds", "Kawasaki Frontale", "Yokohama F. Marinos", "Gamba Osaka", "Cerezo Osaka", "FC Tokyo", "Machida Zelvia", "Nagoya Grampus", "Kyoto Sanga", "Avispa Fukuoka", "Shimizu S-Pulse", "Fagiano Okayama", "Tokyo Verdy", "Albirex Niigata", "Shonan Bellmare", "Yokohama FC", "Júbilo Iwata", "Consadole Sapporo", "Vegalta Sendai", "JEF United", "Omiya Ardija", "V-Varen Nagasaki", "Montedio Yamagata", "Sagan Tosu", "Mito HollyHock", "Tokushima Vortis")),
        CountryDef("KOR", "Güney Kore", false, 26.0, NameGroup.KOREAN, listOf(LeagueDef("K League 1", 12, 0, 1)), "Korea Cup",
            listOf("Jeonbuk Hyundai", "Ulsan HD", "Pohang Steelers", "FC Seoul", "Gangwon FC", "Gimcheon Sangmu", "Daejeon Hana", "Gwangju FC", "Suwon FC", "Jeju SK", "FC Anyang", "Daegu FC")),
        CountryDef("CHN", "Çin", false, 18.0, NameGroup.CHINESE, listOf(LeagueDef("Chinese Super League", 16, 0, 2)), "FA Cup (CHN)",
            listOf("Shanghai Port", "Shanghai Shenhua", "Chengdu Rongcheng", "Beijing Guoan", "Shandong Taishan", "Tianjin Jinmen Tiger", "Zhejiang FC", "Henan FC", "Wuhan Three Towns", "Qingdao West Coast", "Yunnan Yukun", "Shenzhen Peng City", "Dalian Yingbo", "Meizhou Hakka", "Qingdao Hainiu", "Changchun Yatai")),
        CountryDef("AUS", "Avustralya", false, 16.0, NameGroup.ENGLISH, listOf(LeagueDef("A-League", 12, 0, 0)), "Australia Cup",
            listOf("Melbourne City", "Melbourne Victory", "Sydney FC", "Western Sydney Wanderers", "Auckland FC", "Adelaide United", "Central Coast Mariners", "Brisbane Roar", "Macarthur FC", "Newcastle Jets", "Perth Glory", "Wellington Phoenix")),
        CountryDef("EGY", "Mısır", false, 20.0, NameGroup.ARABIC, listOf(LeagueDef("Egyptian Premier League", 18, 0, 3)), "Egypt Cup",
            listOf("Al Ahly", "Zamalek", "Pyramids", "Al Masry", "Future FC", "Ismaily", "ZED", "Ceramica Cleopatra", "Smouha", "ENPPI", "Talaea El Gaish", "Al Ittihad Alexandria", "El Gouna", "Pharco", "National Bank", "Ghazl El Mahalla", "Petrojet", "Wadi Degla")),
        CountryDef("MAR", "Fas", false, 20.0, NameGroup.ARABIC, listOf(LeagueDef("Botola Pro", 16, 0, 2)), "Throne Cup",
            listOf("Wydad Casablanca", "Raja Casablanca", "RS Berkane", "FAR Rabat", "FUS Rabat", "Maghreb Fès", "Hassania Agadir", "IR Tanger", "Difaâ El Jadidi", "Olympic Safi", "Union Touarga", "CODM Meknès", "Moghreb Tétouan", "Kawkab Marrakech", "Renaissance Zemamra", "Chabab Mohammédia")),
        CountryDef("COL", "Kolombiya", false, 22.0, NameGroup.SPANISH, listOf(LeagueDef("Liga BetPlay", 20, 0, 2)), "Copa Colombia",
            listOf("Atlético Nacional", "Millonarios", "América de Cali", "Junior", "Independiente Santa Fe", "Deportivo Cali", "Independiente Medellín", "Once Caldas", "Deportes Tolima", "Atlético Bucaramanga", "Deportivo Pasto", "Envigado", "Alianza", "Águilas Doradas", "La Equidad", "Boyacá Chicó", "Llaneros", "Fortaleza CEIF", "Cúcuta Deportivo", "Unión Magdalena")),
        CountryDef("URU", "Uruguay", false, 18.0, NameGroup.SPANISH, listOf(LeagueDef("Primera División (URU)", 16, 0, 3)), "Copa AUF",
            listOf("Peñarol", "Nacional", "Defensor Sporting", "Liverpool (URU)", "Danubio", "Montevideo Wanderers", "Cerro Largo", "Racing (URU)", "Boston River", "Plaza Colonia", "Progreso", "River Plate (URU)", "Cerro", "Juventud", "Miramar Misiones", "Deportivo Maldonado")),
        CountryDef("CHI", "Şili", false, 15.0, NameGroup.SPANISH, listOf(LeagueDef("Primera División (CHI)", 16, 0, 2)), "Copa Chile",
            listOf("Colo-Colo", "Universidad de Chile", "Universidad Católica", "Palestino", "Cobresal", "Huachipato", "Coquimbo Unido", "Audax Italiano", "O'Higgins", "Everton (CHI)", "Unión Española", "Ñublense", "Deportes Iquique", "Unión La Calera", "Deportes La Serena", "Deportes Limache")),
        CountryDef("QAT", "Katar / BAE", false, 17.0, NameGroup.ARABIC, listOf(LeagueDef("Gulf Stars League", 14, 0, 1)), "Emir Cup",
            listOf("Al-Sadd", "Al-Duhail", "Al-Rayyan", "Al-Gharafa", "Al-Arabi", "Al-Wakrah", "Qatar SC", "Umm Salal", "Al-Ain", "Shabab Al-Ahli", "Al-Wahda", "Al-Jazira", "Sharjah", "Al-Wasl"))
    )

    /** Kulüp sayısı kadar isim listede yoksa şehir listesinden + sonek ile isim üretilir. */
    fun clubSuffix(country: String): List<String> = when (country) {
        "TUR" -> listOf("spor", " FK", " Belediyespor", " İdman Yurdu", " Gençlik")
        "ENG", "SCO", "IRL", "AUS", "USA" -> listOf(" Town", " United", " City", " Rovers", " Athletic", " FC")
        "ESP", "ARG", "MEX", "COL", "URU", "CHI" -> listOf(" CF", " CD", " UD", " Deportivo", " Atlético")
        "GER", "AUT", "SUI" -> listOf(" SV", " FC", " SC", " VfB", " Eintracht")
        "ITA" -> listOf(" Calcio", " AC", " US", " FC")
        "FRA", "BEL" -> listOf(" FC", " AS", " SC", " Olympique", " Stade")
        else -> listOf(" FC", " SC", " United", " Athletic")
    }
}
