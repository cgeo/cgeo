### Avancerat filtreringssystem
- Introduktion av ett nytt filtreringssystem till c:geo, med stöd för flexibla, kombinerbara och lagringsbara filter
- Tillgänglig i både cache-listor och kartvy
- Ny funktion "Sök med filter"

### Karta
- Nyhet: När du skapar en användardefinierad cache när du visar en karta från en lista: Erbjud användaren att lagra den nya cachen i nuvarande lista (i stället för standardlistan för användardefinierade cacher)
- Nyhet: Separera "egna" och "hittade" filter i kartans snabbinställningar
- Förändring: Visa även cachenamn i popup-detaljer

### Cachedetaljer
- Nyhet: Använd Google Översätt som en popup-översättning i appen
- Nyhet: Tillåt att ändra den tilldelade ikonen i cache-detaljer popup via långklick (endast lagrade cacher)

### Nedladdare
- Förändring: Nedladdningar kommer nu att ske helt i bakgrunden, en avisering visas
- Förändring: Filer som laddats ner kommer automatiskt att skriva över befintliga filer med samma namn
- Förändring: Om en karta kräver ett visst tema som inte är installerat ännu kommer c:geo automatiskt ladda ner och installera det temat

### Övrigt
- Förändring: Vi har helt omarbetat de interna tekniska aspekterna c:geo tema för att kunna använda några mer moderna komponenter som tillhandahålls av Android. Detta kommer att ha ett par biverkningar, några av dem oavsiktliga. Rapportera eventuella fel eller fel antingen på vår [GitHub sida](https://www.github.com/cgeo/cgeo/issues) eller genom att kontakta support.
- Nyhet: Support dag / natt läge från systemet (valbart)
- Nyhet: Ladda ner bokmärkeslistor från geocaching.com - se "Listor / pocket querys" i huvudmenyn
- Nyhet: Ignorera förmåga för geocaching.su
- Förändring: Borttagen, RMAPS navigationsapp som inte längre underhålls
- Fix: Extrahera waypoint med samma namn men olika koordinater från personlig anteckning
- Fix: Bugg i extrahering av användaranteckning för waypoint med formel
- Fix: Exportera formel till PN istället för koordinater för färdig formel
- Fix: Mapp för offline kartor och teman felaktig efter ominstallation och återställning av säkerhetskopia
- Fix: Spårning/rutt kan inte uppdateras
- Fix: Fel vid nedladdning i ljust tema
