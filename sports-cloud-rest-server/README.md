JSON Proxy Server
==================

# How to Build & Run
mvn clean package

cd target

nohup java -Xmx1g -Xms1g  -Dtarget-host-to-proxy=http://gwserv-mobileprod.echodata.tv -Djson-path-file=/data/config/json-proxy-server/json-path-file.txt -jar json-proxy-server.jar localhost 5001 &

# TroubleShooting
Restart the server if you see that the json path does not work. Will be updated in next release.


# Running with docker

Build the docker image

docker build -t sling-json-proxy-dev .

docker network create --driver bridge sling-sport-cloud

```bash
docker run --network='sling-sport-cloud' -it -p 5001:5001  -e HOST_GID=`id -g` -e HOST_UID=`id -u` -e HOST_USER='arung' -v ~/.m2:/root/.m2 -v /Users/arung/backend/development/projects/sling/git/sports-cloud/test-fixtures/json-proxy-server:/project sling-json-proxy-dev 
```
Once logged in run the following

```bash
mvn clean package
cd target
java -Xmx1g -Xms1g  -Dtarget-host-to-proxy=http://gwserv-mobileprod.echodata.tv -Djson-path-file=/data/config/json-proxy-server/json-path-file.txt -jar json-proxy-server.jar localhost 5001
```
# More Information

Copy the file from the resources folder to /data/config/json-proxy-server/json-path-file.txt and change appropriately

## Config File Format 

This follows the JSON Path syntax as documented in https://github.com/json-path/JsonPath

(Path to search with filter conditions)<::>(Offset Path to replace from Search tree)<::>replacement Value<::>(Replacement Type)

Example is available inside source code @ src/main/resource/json-path-file.txt

## Flavours
### Search for a predicate like  the following  and replace value of another field
$.content[?(@.homeTeam.name == 'Spurs' && @.awayTeam.name == 'Rockets' && @.sport == 'basketball' && @.league == 'nba')]<::>.scheduledDate<::>Thu, 11 May 2017 12:30:00 +0000<::>PREDICATE_REPLACE

The above expression when put in config file searches for all the games with home team name as 'Spurs' , away team name as 'basketball' , sport as 'basketball' and 'league' is 'nba' , replace the scheduledDate field with the new value

### Replace all fields for a specific json path 
<::>$.content[*].league<::>nba<::>BLIND_REPLACE

The above expression when put in config will replace all the 'league' property with value 'nba'

# JsonPath
We use JsonPath library to implement the json path

Path Examples
-------------

Given the json

```javascript
{
    "store": {
        "book": [
            {
                "category": "reference",
                "author": "Nigel Rees",
                "title": "Sayings of the Century",
                "price": 8.95
            },
            {
                "category": "fiction",
                "author": "Evelyn Waugh",
                "title": "Sword of Honour",
                "price": 12.99
            },
            {
                "category": "fiction",
                "author": "Herman Melville",
                "title": "Moby Dick",
                "isbn": "0-553-21311-3",
                "price": 8.99
            },
            {
                "category": "fiction",
                "author": "J. R. R. Tolkien",
                "title": "The Lord of the Rings",
                "isbn": "0-395-19395-8",
                "price": 22.99
            }
        ],
        "bicycle": {
            "color": "red",
            "price": 19.95
        }
    },
    "expensive": 10
}
```

| JsonPath (click link to try)| Result |
| :------- | :----- |
| <a href="http://jsonpath.herokuapp.com/?path=$.store.book[*].author" target="_blank">$.store.book[*].author</a>| The authors of all books     |
| <a href="http://jsonpath.herokuapp.com/?path=$..author" target="_blank">$..author</a>                   | All authors                         |
| <a href="http://jsonpath.herokuapp.com/?path=$.store.*" target="_blank">$.store.*</a>                  | All things, both books and bicycles  |
| <a href="http://jsonpath.herokuapp.com/?path=$.store..price" target="_blank">$.store..price</a>             | The price of everything         |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[2]" target="_blank">$..book[2]</a>                 | The third book                      |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[2]" target="_blank">$..book[-2]</a>                 | The second to last book            |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[0,1]" target="_blank">$..book[0,1]</a>               | The first two books               |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[:2]" target="_blank">$..book[:2]</a>                | All books from index 0 (inclusive) until index 2 (exclusive) |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[1:2]" target="_blank">$..book[1:2]</a>                | All books from index 1 (inclusive) until index 2 (exclusive) |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[-2:]" target="_blank">$..book[-2:]</a>                | Last two books                   |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[2:]" target="_blank">$..book[2:]</a>                | Book number two from tail          |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[?(@.isbn)]" target="_blank">$..book[?(@.isbn)]</a>          | All books with an ISBN number         |
| <a href="http://jsonpath.herokuapp.com/?path=$.store.book[?(@.price < 10)]" target="_blank">$.store.book[?(@.price < 10)]</a> | All books in store cheaper than 10  |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[?(@.price <= $['expensive'])]" target="_blank">$..book[?(@.price <= $['expensive'])]</a> | All books in store that are not "expensive"  |
| <a href="http://jsonpath.herokuapp.com/?path=$..book[?(@.author =~ /.*REES/i)]" target="_blank">$..book[?(@.author =~ /.*REES/i)]</a> | All books matching regex (ignore case)  |
| <a href="http://jsonpath.herokuapp.com/?path=$..*" target="_blank">$..*</a>                        | Give me every thing   
| <a href="http://jsonpath.herokuapp.com/?path=$..book.length()" target="_blank">$..book.length()</a>                 | The number of books                      |

