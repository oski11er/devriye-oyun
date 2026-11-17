package com.futbolmenajer.data.db

import kotlin.random.Random

enum class NameGroup { ENGLISH, SPANISH, PORTUGUESE, GERMAN, ITALIAN, FRENCH, DUTCH, TURKISH, NORDIC, SLAVIC_WEST, SLAVIC_SOUTH, SLAVIC_EAST, GREEK, ROMANIAN, HUNGARIAN, HEBREW, ARABIC, JAPANESE, KOREAN, CHINESE, WEST_AFRICAN }

/** Uyruk kodları → Türkçe ülke adı ve isim grubu (milli takımlar da buradan üretilir). */
data class Nation(val code: String, val nameTr: String, val group: NameGroup, val strength: Int)

object NameBank {

    val nations: List<Nation> = listOf(
        Nation("ENG", "İngiltere", NameGroup.ENGLISH, 88), Nation("ESP", "İspanya", NameGroup.SPANISH, 90),
        Nation("GER", "Almanya", NameGroup.GERMAN, 86), Nation("ITA", "İtalya", NameGroup.ITALIAN, 84),
        Nation("FRA", "Fransa", NameGroup.FRENCH, 90), Nation("POR", "Portekiz", NameGroup.PORTUGUESE, 86),
        Nation("NED", "Hollanda", NameGroup.DUTCH, 84), Nation("BEL", "Belçika", NameGroup.DUTCH, 80),
        Nation("TUR", "Türkiye", NameGroup.TURKISH, 78), Nation("CRO", "Hırvatistan", NameGroup.SLAVIC_SOUTH, 80),
        Nation("SRB", "Sırbistan", NameGroup.SLAVIC_SOUTH, 74), Nation("SUI", "İsviçre", NameGroup.GERMAN, 76),
        Nation("AUT", "Avusturya", NameGroup.GERMAN, 76), Nation("DEN", "Danimarka", NameGroup.NORDIC, 77),
        Nation("NOR", "Norveç", NameGroup.NORDIC, 76), Nation("SWE", "İsveç", NameGroup.NORDIC, 72),
        Nation("POL", "Polonya", NameGroup.SLAVIC_WEST, 72), Nation("CZE", "Çekya", NameGroup.SLAVIC_WEST, 71),
        Nation("UKR", "Ukrayna", NameGroup.SLAVIC_EAST, 73), Nation("SCO", "İskoçya", NameGroup.ENGLISH, 70),
        Nation("WAL", "Galler", NameGroup.ENGLISH, 69), Nation("IRL", "İrlanda", NameGroup.ENGLISH, 66),
        Nation("GRE", "Yunanistan", NameGroup.GREEK, 70), Nation("ROU", "Romanya", NameGroup.ROMANIAN, 69),
        Nation("HUN", "Macaristan", NameGroup.HUNGARIAN, 70), Nation("SVK", "Slovakya", NameGroup.SLAVIC_WEST, 67),
        Nation("SVN", "Slovenya", NameGroup.SLAVIC_SOUTH, 67), Nation("ISR", "İsrail", NameGroup.HEBREW, 62),
        Nation("BUL", "Bulgaristan", NameGroup.SLAVIC_EAST, 60), Nation("FIN", "Finlandiya", NameGroup.NORDIC, 61),
        Nation("CYP", "Kıbrıs", NameGroup.GREEK, 52), Nation("AZE", "Azerbaycan", NameGroup.TURKISH, 52),
        Nation("KAZ", "Kazakistan", NameGroup.SLAVIC_EAST, 54), Nation("MKD", "Kuzey Makedonya", NameGroup.SLAVIC_SOUTH, 58),
        Nation("RUS", "Rusya", NameGroup.SLAVIC_EAST, 70), Nation("BIH", "Bosna-Hersek", NameGroup.SLAVIC_SOUTH, 63),
        Nation("ALB", "Arnavutluk", NameGroup.SLAVIC_SOUTH, 62), Nation("GEO", "Gürcistan", NameGroup.SLAVIC_EAST, 66),
        Nation("BRA", "Brezilya", NameGroup.PORTUGUESE, 89), Nation("ARG", "Arjantin", NameGroup.SPANISH, 91),
        Nation("URU", "Uruguay", NameGroup.SPANISH, 82), Nation("COL", "Kolombiya", NameGroup.SPANISH, 80),
        Nation("CHI", "Şili", NameGroup.SPANISH, 70), Nation("ECU", "Ekvador", NameGroup.SPANISH, 76),
        Nation("PAR", "Paraguay", NameGroup.SPANISH, 72), Nation("PER", "Peru", NameGroup.SPANISH, 66),
        Nation("MEX", "Meksika", NameGroup.SPANISH, 76), Nation("USA", "ABD", NameGroup.ENGLISH, 76),
        Nation("CAN", "Kanada", NameGroup.ENGLISH, 72), Nation("JPN", "Japonya", NameGroup.JAPANESE, 79),
        Nation("KOR", "Güney Kore", NameGroup.KOREAN, 75), Nation("AUS", "Avustralya", NameGroup.ENGLISH, 70),
        Nation("KSA", "Suudi Arabistan", NameGroup.ARABIC, 64), Nation("IRN", "İran", NameGroup.ARABIC, 70),
        Nation("QAT", "Katar", NameGroup.ARABIC, 60), Nation("CHN", "Çin", NameGroup.CHINESE, 52),
        Nation("EGY", "Mısır", NameGroup.ARABIC, 70), Nation("MAR", "Fas", NameGroup.ARABIC, 82),
        Nation("ALG", "Cezayir", NameGroup.ARABIC, 72), Nation("TUN", "Tunus", NameGroup.ARABIC, 66),
        Nation("SEN", "Senegal", NameGroup.WEST_AFRICAN, 78), Nation("NGA", "Nijerya", NameGroup.WEST_AFRICAN, 74),
        Nation("CIV", "Fildişi Sahili", NameGroup.WEST_AFRICAN, 76), Nation("GHA", "Gana", NameGroup.WEST_AFRICAN, 70),
        Nation("CMR", "Kamerun", NameGroup.WEST_AFRICAN, 70), Nation("MLI", "Mali", NameGroup.WEST_AFRICAN, 70)
    )
    val nationByCode = nations.associateBy { it.code }

    /** Ülke ligindeki yabancı oyuncular hangi uyruklardan gelir (ağırlıklı). */
    fun foreignPool(leagueCountry: String): List<String> = when (leagueCountry) {
        "ENG", "SCO" -> listOf("FRA", "ESP", "BRA", "POR", "NED", "BEL", "ARG", "GER", "SEN", "NGA", "CIV", "DEN", "NOR", "IRL", "WAL", "SCO", "USA", "JPN", "GHA", "URU", "COL", "CRO", "SUI")
        "TUR" -> listOf("BRA", "POR", "SEN", "NGA", "CIV", "SRB", "CRO", "BIH", "ALB", "AZE", "NED", "BEL", "ARG", "MAR", "ALG", "POL", "CZE", "GEO", "GER", "FRA", "ESP", "ITA", "COL", "URU")
        "KSA", "QAT" -> listOf("BRA", "POR", "FRA", "ESP", "SEN", "MAR", "ALG", "EGY", "CRO", "SRB", "COL", "ARG", "ENG", "BEL", "NED")
        "USA", "MEX" -> listOf("ARG", "COL", "URU", "ECU", "PAR", "MEX", "USA", "CAN", "BRA", "ESP", "ENG", "GER", "JPN", "KOR")
        "BRA", "ARG", "COL", "URU", "CHI" -> listOf("ARG", "URU", "COL", "PAR", "ECU", "CHI", "PER", "BRA")
        "JPN", "KOR", "CHN", "AUS" -> listOf("BRA", "KOR", "JPN", "AUS", "SRB", "ESP", "ARG", "NGA", "GHA")
        else -> listOf("BRA", "FRA", "SEN", "NGA", "CRO", "SRB", "POR", "ESP", "ARG", "NED", "BEL", "GER", "CIV", "GHA", "MAR", "BIH", "ALB", "GEO", "UKR", "POL")
    }

    private val first: Map<NameGroup, List<String>> = mapOf(
        NameGroup.ENGLISH to listOf("James", "Jack", "Harry", "Oliver", "George", "Charlie", "Thomas", "Callum", "Lewis", "Ryan", "Jordan", "Kyle", "Mason", "Declan", "Jude", "Cole", "Reece", "Aaron", "Ben", "Luke", "Josh", "Liam", "Connor", "Ethan", "Tyler", "Jamie", "Marcus", "Kieran", "Danny", "Sam", "Joe", "Adam", "Max", "Alfie", "Archie", "Morgan", "Rhys", "Dylan", "Owen", "Nathan"),
        NameGroup.SPANISH to listOf("Alejandro", "Pablo", "Sergio", "Carlos", "Javier", "Álvaro", "Daniel", "Diego", "Mario", "Iván", "Raúl", "Adrián", "Rodrigo", "Marcos", "Hugo", "Martín", "Nicolás", "Lucas", "Mateo", "Santiago", "Joaquín", "Gonzalo", "Facundo", "Emiliano", "Julián", "Lautaro", "Enzo", "Thiago", "Agustín", "Juan", "Luis", "Fernando", "Andrés", "Jorge", "Miguel", "Rubén", "Óscar", "Íñigo", "Unai", "Pedro"),
        NameGroup.PORTUGUESE to listOf("João", "Rafael", "Bruno", "Diogo", "Gonçalo", "Rúben", "Tiago", "André", "Francisco", "Pedro", "Vitinha", "Nuno", "Gabriel", "Lucas", "Matheus", "Vinícius", "Rodrygo", "Gustavo", "Felipe", "Thiago", "Endrick", "Raphinha", "Éder", "Danilo", "Bernardo", "Ricardo", "Fábio", "Hugo", "Paulinho", "Igor", "Murilo", "Caio", "Wesley", "Alisson", "Marquinhos", "Neto", "Renan", "Leandro", "Douglas", "Henrique"),
        NameGroup.GERMAN to listOf("Lukas", "Leon", "Jonas", "Felix", "Florian", "Kai", "Joshua", "Niklas", "Maximilian", "Julian", "Tim", "Jan", "David", "Marc", "Nico", "Robin", "Kevin", "Pascal", "Dominik", "Moritz", "Fabian", "Tobias", "Simon", "Elias", "Jamal", "Leroy", "Serge", "Manuel", "Thomas", "Philipp", "Benjamin", "Marco", "Yann", "Granit", "Nils", "Lennart", "Paul", "Aleksandar", "Stefan", "Christoph"),
        NameGroup.ITALIAN to listOf("Lorenzo", "Federico", "Alessandro", "Matteo", "Nicolò", "Davide", "Gianluigi", "Andrea", "Marco", "Giacomo", "Sandro", "Riccardo", "Francesco", "Leonardo", "Gianluca", "Simone", "Stefano", "Manuel", "Mattia", "Samuele", "Tommaso", "Pietro", "Daniele", "Ciro", "Domenico", "Giovanni", "Luca", "Fabio", "Raoul", "Destiny", "Wilfried", "Mario", "Moise", "Bryan", "Cristiano", "Alessio", "Giorgio", "Edoardo", "Emanuele", "Antonio"),
        NameGroup.FRENCH to listOf("Kylian", "Antoine", "Ousmane", "Aurélien", "Eduardo", "Theo", "Lucas", "Jules", "William", "Dayot", "Randal", "Adrien", "Mattéo", "Bradley", "Warren", "Désiré", "Michael", "Rayan", "Hugo", "Mike", "Benjamin", "Jonathan", "Youssouf", "Christopher", "Marcus", "Ibrahima", "Moussa", "Kingsley", "Maxence", "Florian", "Loïs", "Malo", "Elye", "Wesley", "Kolo", "Leny", "Mathys", "Enzo", "Nathan", "Quentin"),
        NameGroup.DUTCH to listOf("Virgil", "Frenkie", "Matthijs", "Cody", "Xavi", "Ryan", "Denzel", "Tijjani", "Jurriën", "Micky", "Bart", "Stefan", "Wout", "Teun", "Joey", "Jeremie", "Kevin", "Romelu", "Youri", "Leandro", "Amadou", "Charles", "Arthur", "Lois", "Jan", "Thibaut", "Sem", "Daley", "Steven", "Memphis", "Noa", "Mats", "Quinten", "Brian", "Justin", "Jorrel", "Ian", "Calvin", "Mitchel", "Owen"),
        NameGroup.TURKISH to listOf("Arda", "Kerem", "Hakan", "Ferdi", "Orkun", "Kenan", "Barış", "Abdülkerim", "Mert", "Uğurcan", "Altay", "Zeki", "Çağlar", "Merih", "Salih", "İsmail", "Yunus", "Kaan", "Semih", "Cenk", "Oğuz", "Emre", "Burak", "Okay", "Eren", "Can", "Berkan", "Doğukan", "Efe", "Yusuf", "Bertuğ", "Halil", "Enes", "Umut", "Ahmet", "Mehmet", "Ozan", "Serdar", "Taylan", "Atakan", "Ali", "Ömer", "Furkan", "Kerim", "Samet", "Rıdvan", "Onur", "Tolga", "Batuhan", "Eray"),
        NameGroup.NORDIC to listOf("Erling", "Martin", "Alexander", "Rasmus", "Christian", "Pierre", "Kasper", "Joachim", "Andreas", "Viktor", "Mikkel", "Jonas", "Oscar", "Sander", "Kristoffer", "Emil", "Isak", "Anders", "Lucas", "Mathias", "Jens", "Oliver", "Gustav", "Hugo", "Elias", "Albert", "Morten", "Yasin", "Dejan", "Patrik", "Teemu", "Lukas", "Glen", "Aron", "Hákon", "Ola", "Fredrik", "Magnus", "Thomas", "Henrik"),
        NameGroup.SLAVIC_WEST to listOf("Robert", "Piotr", "Jakub", "Kamil", "Wojciech", "Mateusz", "Krzysztof", "Przemysław", "Tomáš", "Patrik", "Adam", "Vladimír", "Ondřej", "Lukáš", "Martin", "Jan", "Milan", "Stanislav", "Dávid", "Marek", "Michal", "Filip", "Bartosz", "Sebastian", "Karol", "Nicola", "Szymon", "Dominik", "Jiří", "Pavel"),
        NameGroup.SLAVIC_SOUTH to listOf("Luka", "Ivan", "Josip", "Mateo", "Marcelo", "Joško", "Andrej", "Dušan", "Aleksandar", "Sergej", "Nemanja", "Filip", "Strahinja", "Lazar", "Dejan", "Nikola", "Miloš", "Marko", "Mario", "Edin", "Ermedin", "Amar", "Jan", "Benjamin", "Elif", "Armando", "Kristjan", "Bajram", "Enis", "Eljif"),
        NameGroup.SLAVIC_EAST to listOf("Mykhailo", "Artem", "Oleksandr", "Andriy", "Vitaliy", "Roman", "Illia", "Georgiy", "Ruslan", "Taras", "Aleksey", "Dmitri", "Sergei", "Igor", "Maksim", "Nikita", "Anton", "Kirill", "Khvicha", "Giorgi", "Zuriko", "Ivaylo", "Kiril", "Bauyrzhan", "Islambek", "Yuri", "Viktor", "Denis", "Vladislav", "Matvei"),
        NameGroup.GREEK to listOf("Giorgos", "Konstantinos", "Dimitris", "Vangelis", "Kostas", "Anastasios", "Christos", "Nikos", "Petros", "Fotis", "Manolis", "Odysseas", "Pantelis", "Andreas", "Ioannis", "Lazaros", "Charalampos", "Stefanos", "Panagiotis", "Thanasis"),
        NameGroup.ROMANIAN to listOf("Radu", "Nicolae", "Ianis", "Răzvan", "Florin", "Denis", "Dennis", "Andrei", "Alexandru", "Vlad", "Ștefan", "Marius", "Darius", "Valentin", "Bogdan", "Cristian", "Mihai", "Adrian", "Ionuț", "Octavian"),
        NameGroup.HUNGARIAN to listOf("Dominik", "Péter", "Ádám", "Willi", "Roland", "Milos", "Bendegúz", "Attila", "Barnabás", "Kevin", "Zsolt", "Dániel", "Márton", "Botond", "Callum", "Loïc", "Bence", "Gergő", "Tamás", "Krisztián"),
        NameGroup.HEBREW to listOf("Eran", "Manor", "Oscar", "Dor", "Liel", "Mohammad", "Gavriel", "Omri", "Yoav", "Eli", "Shon", "Dan", "Itay", "Roy", "Tomer", "Idan", "Nir", "Ofir", "Yonatan", "Raz"),
        NameGroup.ARABIC to listOf("Mohamed", "Ahmed", "Omar", "Youssef", "Hakim", "Achraf", "Sofyan", "Riyad", "Ismaël", "Salem", "Firas", "Abdullah", "Saud", "Hassan", "Karim", "Mahmoud", "Amine", "Bilal", "Nayef", "Azzedine", "Brahim", "Mehdi", "Yassine", "Ayoub", "Ali", "Sami", "Hamza", "Ilias", "Walid", "Rami"),
        NameGroup.JAPANESE to listOf("Takefusa", "Kaoru", "Wataru", "Daichi", "Takehiro", "Ritsu", "Junya", "Kyogo", "Ayase", "Hiroki", "Ko", "Takumi", "Yukinari", "Zion", "Keito", "Daizen", "Shuto", "Koki", "Yuito", "Reo", "Hidemasa", "Kaishu", "Yuki", "Sota", "Ao", "Kento", "Shogo", "Ryo", "Haruki", "Tomoki"),
        NameGroup.KOREAN to listOf("Heung-min", "Kang-in", "Min-jae", "Hee-chan", "Gue-sung", "Jae-sung", "In-beom", "Seung-ho", "Woo-yeong", "Young-woo", "Hyeon-gyu", "Seung-gyu", "Jin-su", "Ji-sung", "Jun-ho", "Tae-hwan", "Ji-soo", "Dong-gyeong", "Hyun-jun", "Sang-ho"),
        NameGroup.CHINESE to listOf("Wu", "Wei", "Zhang", "Yan", "Jiang", "Liu", "Lin", "Zhu", "Wang", "Tan", "Bai", "Xie", "Fei", "Li", "Chen", "Hao", "Peng", "Gao", "Yang", "Ma"),
        NameGroup.WEST_AFRICAN to listOf("Sadio", "Kalidou", "Idrissa", "Ismaïla", "Nicolas", "Victor", "Ademola", "Samuel", "Wilfred", "Alex", "Franck", "Sébastien", "Simon", "Seko", "Amad", "Mohammed", "Thomas", "Iñaki", "Antoine", "André", "Bryan", "Yves", "Moussa", "Cheick", "Amadou", "Boubacar", "Lamine", "Pape", "Habib", "Krépin")
    )

    private val last: Map<NameGroup, List<String>> = mapOf(
        NameGroup.ENGLISH to listOf("Smith", "Jones", "Taylor", "Brown", "Williams", "Wilson", "Johnson", "Davies", "Robinson", "Wright", "Thompson", "Evans", "Walker", "White", "Roberts", "Green", "Hall", "Wood", "Jackson", "Clarke", "Harrison", "Palmer", "Foden", "Rice", "Kane", "Stones", "Pickford", "Mainoo", "Gordon", "Watkins", "Bowen", "Toney", "Gibbs", "Branthwaite", "Colwill", "Madueke", "Eze", "Wharton", "Quansah", "Lewis", "McAllister", "Ferguson", "McGinn", "Robertson", "Tierney", "Gilmour", "Doak", "Hughes", "Bennett", "Cooper"),
        NameGroup.SPANISH to listOf("García", "Martínez", "López", "Sánchez", "Pérez", "González", "Rodríguez", "Fernández", "Gómez", "Díaz", "Romero", "Álvarez", "Torres", "Ruiz", "Navarro", "Moreno", "Herrera", "Castro", "Ortiz", "Vázquez", "Ramos", "Molina", "Suárez", "Delgado", "Morales", "Medina", "Aguirre", "Gutiérrez", "Rojas", "Silva", "Mendoza", "Paredes", "Acuña", "Correa", "Merino", "Zubimendi", "Oyarzabal", "Pedri", "Gavi", "Cubarsí", "Olmo", "Baena", "Llorente", "Valverde", "Ugarte", "Arango", "Lozano", "Montes", "Vega", "Bravo"),
        NameGroup.PORTUGUESE to listOf("Silva", "Santos", "Ferreira", "Pereira", "Oliveira", "Costa", "Rodrigues", "Martins", "Sousa", "Fernandes", "Gonçalves", "Gomes", "Lopes", "Marques", "Alves", "Almeida", "Ribeiro", "Pinto", "Carvalho", "Teixeira", "Moreira", "Correia", "Mendes", "Nunes", "Soares", "Vieira", "Monteiro", "Cardoso", "Rocha", "Neves", "Leão", "Dias", "Inácio", "Trincão", "Ramos", "Barbosa", "Araújo", "Nascimento", "Lima", "Souza"),
        NameGroup.GERMAN to listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann", "Koch", "Richter", "Klein", "Wolf", "Neumann", "Schwarz", "Zimmermann", "Braun", "Krüger", "Hartmann", "Lange", "Werner", "Krause", "Lehmann", "Kimmich", "Wirtz", "Musiala", "Havertz", "Rüdiger", "Tah", "Schlotterbeck", "Raum", "Mittelstädt", "Undav", "Woltemade", "Pavlović", "Baumann", "Nübel", "Gnabry", "Sané", "Brandt", "Gross", "Xhaka", "Akanji", "Sommer", "Kobel", "Sabitzer", "Laimer", "Baumgartner", "Schlager"),
        NameGroup.ITALIAN to listOf("Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci", "Marino", "Greco", "Bruno", "Gallo", "Conti", "De Luca", "Mancini", "Costa", "Giordano", "Rizzo", "Lombardi", "Moretti", "Barella", "Tonali", "Bastoni", "Donnarumma", "Chiesa", "Retegui", "Kean", "Frattesi", "Calafiori", "Di Lorenzo", "Dimarco", "Locatelli", "Ricci", "Scamacca", "Raspadori", "Pellegrini", "Cambiaso", "Buongiorno", "Udogie", "Vicario"),
        NameGroup.FRENCH to listOf("Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fournier", "Mbappé", "Griezmann", "Dembélé", "Tchouaméni", "Camavinga", "Koundé", "Saliba", "Upamecano", "Konaté", "Maignan", "Rabiot", "Kanté", "Olise", "Doué", "Barcola", "Cherki", "Zaïre-Emery", "Kolo Muani", "Thuram", "Coman"),
        NameGroup.DUTCH to listOf("de Jong", "Jansen", "de Vries", "van den Berg", "van Dijk", "Bakker", "Janssen", "Visser", "Smit", "Meijer", "de Boer", "Mulder", "de Groot", "Bos", "Vos", "Peters", "Hendriks", "van Leeuwen", "Dekker", "Brouwer", "de Ligt", "Gakpo", "Simons", "Reijnders", "Gravenberch", "Timber", "Dumfries", "Frimpong", "Malen", "Weghorst", "Verbruggen", "Koopmeiners", "De Ketelaere", "Doku", "Tielemans", "Onana", "Openda", "Trossard", "Courtois", "Mangala"),
        NameGroup.TURKISH to listOf("Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir", "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek", "Güler", "Aktürkoğlu", "Çalhanoğlu", "Kadıoğlu", "Kökçü", "Akaydın", "Bardakcı", "Çakır", "Bayındır", "Kabak", "Ayhan", "Çelik", "Özcan", "Yazıcı", "Akgün", "Ünder", "Kahveci", "Yüksek", "Elmalı", "Müldür", "Kılıçsoy", "Uzun", "Aydoğdu", "Yokuşlu", "Tosun", "Karaman", "Topçu", "Bulut", "Sarı", "Ertuğrul"),
        NameGroup.NORDIC to listOf("Haaland", "Ødegaard", "Sørloth", "Højlund", "Eriksen", "Højbjerg", "Schmeichel", "Christensen", "Andersen", "Nielsen", "Hansen", "Pedersen", "Larsen", "Johansson", "Karlsson", "Nilsson", "Eriksson", "Isak", "Gyökeres", "Kulusevski", "Lindelöf", "Elanga", "Berg", "Nusa", "Bobb", "Ryerson", "Strand Larsen", "Wind", "Dorgu", "Hjulmand", "Damsgaard", "Olsen", "Pukki", "Hradecky", "Kamara", "Jónsson", "Sigurðsson", "Thorsby", "Aursnes", "Hauge"),
        NameGroup.SLAVIC_WEST to listOf("Lewandowski", "Zieliński", "Szczęsny", "Kiwior", "Cash", "Świderski", "Piątek", "Bednarek", "Frankowski", "Moder", "Souček", "Schick", "Coufal", "Holeš", "Hložek", "Černý", "Provod", "Kovář", "Staněk", "Krejčí", "Škriniar", "Lobotka", "Haraslín", "Duda", "Hancko", "Dúbravka", "Kowalski", "Nowak", "Wiśniewski", "Novák"),
        NameGroup.SLAVIC_SOUTH to listOf("Modrić", "Kovačić", "Gvardiol", "Perišić", "Kramarić", "Brozović", "Livaković", "Šutalo", "Baturina", "Sučić", "Vlahović", "Mitrović", "Milinković-Savić", "Tadić", "Pavlović", "Lukić", "Gudelj", "Kostić", "Džeko", "Kolašinac", "Demirović", "Šeško", "Oblak", "Elsnik", "Broja", "Asllani", "Hysaj", "Bajrami", "Elmas", "Bardhi"),
        NameGroup.SLAVIC_EAST to listOf("Mudryk", "Dovbyk", "Zinchenko", "Tsygankov", "Sudakov", "Trubin", "Zabarnyi", "Mykolenko", "Malinovskyi", "Yaremchuk", "Golovin", "Chalov", "Safonov", "Batrakov", "Kvaratskhelia", "Mamardashvili", "Mikautadze", "Chakvetadze", "Kochorashvili", "Despodov", "Zuev", "Ivanov", "Petrov", "Sidorov", "Smirnov", "Popov", "Volkov", "Kuznetsov", "Sokolov", "Zainutdinov"),
        NameGroup.GREEK to listOf("Papadopoulos", "Tsimikas", "Bakasetas", "Pavlidis", "Mavropanos", "Vlachodimos", "Masouras", "Mantalos", "Koulierakis", "Tzolis", "Karetsas", "Zafeiris", "Kourbelis", "Siopis", "Giakoumakis", "Ioannidis", "Rota", "Hatzidiakos", "Georgiou", "Nikolaou"),
        NameGroup.ROMANIAN to listOf("Drăgușin", "Hagi", "Man", "Mihăilă", "Stanciu", "Rațiu", "Niță", "Marin", "Puşcaş", "Burcă", "Mitriță", "Coman", "Popescu", "Ionescu", "Dumitru", "Stan", "Munteanu", "Birligea", "Olaru", "Bîrligea"),
        NameGroup.HUNGARIAN to listOf("Szoboszlai", "Gulácsi", "Orbán", "Kerkez", "Sallai", "Schäfer", "Nagy", "Varga", "Kovács", "Tóth", "Szabó", "Horváth", "Fiola", "Bolla", "Dárdai", "Styles", "Nego", "Kleinheisler", "Gazdag", "Ádám"),
        NameGroup.HEBREW to listOf("Zahavi", "Solomon", "Gloukh", "Peretz", "Abada", "Abu Fani", "Kanichowsky", "Lavi", "Dasa", "Biton", "Weissman", "Turgeman", "Baribo", "Revivo", "Haziza", "Cohen", "Levi", "Mizrahi", "Friedman", "Azoulay"),
        NameGroup.ARABIC to listOf("Salah", "Marmoush", "Hakimi", "Ziyech", "Amrabat", "En-Nesyri", "Bounou", "Mazraoui", "Aguerd", "Brahim", "Ezzalzouli", "Mahrez", "Bennacer", "Aït-Nouri", "Gouiri", "Al-Dawsari", "Al-Owais", "Al-Shehri", "Kanno", "Taremi", "Azmoun", "Afif", "Trezeguet", "Elneny", "Msakni", "Skhiri", "Laïdouni", "El Khannouss", "Diaz", "Saibari"),
        NameGroup.JAPANESE to listOf("Kubo", "Mitoma", "Endo", "Kamada", "Tomiyasu", "Doan", "Ito", "Furuhashi", "Ueda", "Minamino", "Itakura", "Tanaka", "Machida", "Suzuki", "Sano", "Maeda", "Nakamura", "Takai", "Hatate", "Watanabe", "Sato", "Takahashi", "Kobayashi", "Yamamoto", "Morita", "Hashioka", "Nagatomo", "Asano", "Moriyasu", "Kato"),
        NameGroup.KOREAN to listOf("Son", "Lee", "Kim", "Hwang", "Cho", "Park", "Jeong", "Paik", "Seol", "Oh", "Bae", "Yang", "Choi", "Kang", "Jung", "Yoon", "Jang", "Lim", "Han", "Shin"),
        NameGroup.CHINESE to listOf("Lei", "Shihao", "Xizhe", "Binbin", "Guangtai", "Yuning", "Junmin", "Chenjie", "Shangyuan", "Long", "Yuan", "Jiahao", "Peng", "Haoran", "Tianyi", "Xiaobin", "Zhiyuan", "Yiming", "Ming", "Hao"),
        NameGroup.WEST_AFRICAN to listOf("Mané", "Koulibaly", "Gueye", "Sarr", "Jackson", "Osimhen", "Lookman", "Chukwueze", "Ndidi", "Iwobi", "Kessié", "Haller", "Adingra", "Diallo", "Fofana", "Kudus", "Partey", "Semenyo", "Williams", "Onana", "Mbeumo", "Anguissa", "Bissouma", "Traoré", "Doucouré", "Camara", "Diop", "Ndiaye", "Mendy", "Touré")
    )

    /** Üretilen oyunculara ünlü gerçek futbolcuların soyadı verilmez (karışıklığı önlemek için). */
    private val common = setOf("Silva", "Santos", "Fernandes", "Martínez", "García", "Pereira", "Costa", "Gomes", "Rodríguez",
        "González", "López", "Sánchez", "Pérez", "Fernández", "Díaz", "Torres", "Álvarez", "Romero", "Hernández", "Kim", "Lee",
        "Park", "Smith", "Johnson", "Jones", "Williams", "Brown", "Diallo", "Camara", "Traoré", "Müller", "Rossi", "Ramos",
        "Martins", "Nunes", "Oliveira", "Neves", "Dias", "Lima", "Diop", "Fofana", "Moreno", "Gutiérrez", "Herrera", "Ruiz", "Navarro")
    private val famous: Set<String> by lazy {
        RealPlayers.allClubs().flatMap { RealPlayers.parse(it) }.map { it.last.substringAfterLast(' ') }.filter { it !in common }.toSet() +
            setOf("Mbappé", "Haaland", "Kane", "Yamal", "Messi", "Ronaldo", "Kvaratskhelia", "Doku", "Musiala", "Wirtz", "Salah",
                "Griezmann", "Lewandowski", "Modrić", "Bellingham", "Saka", "Palmer", "Foden", "Rice", "Pedri", "Gavi")
    }

    fun randomName(group: NameGroup, rnd: Random): Pair<String, String> {
        val f = first.getValue(group)
        val l = last.getValue(group)
        var ln = l[rnd.nextInt(l.size)]
        var tries = 0
        while (ln.substringAfterLast(' ') in famous && tries++ < 12) ln = l[rnd.nextInt(l.size)]
        return f[rnd.nextInt(f.size)] to ln
    }

    fun groupFor(code: String): NameGroup = nationByCode[code]?.group ?: NameGroup.ENGLISH
}
