const { error } = require('console');
const fs = require('fs')

var lastRecord = null;
var lastCoordinates = null;

//Saves new record in a JSON file stored locally on the server
function saveNewRecord(record){

    console.log(record)
    
    var allRecords = {
        records: []
    };

    var newCoordinates;
    var newRecord = record

    //We read the file stored on the server and append the new records
    fs.readFile('records.json', function (err, data) {
        if (err) throw err;
        else{
            //If the file is not empty
            if(data != ""){

                allRecords = JSON.parse(data);
                allRecords.records.push(newRecord);

                //We check if there is enough difference between the coordinates to save them in the file
                lastRecord = allRecords.records[allRecords.records.length-2];
                lastLatitude = Math.abs(parseFloat(lastRecord.latitude));
                lastLongitude = Math.abs(parseFloat(lastRecord.longitude));
                newLatitude = Math.abs(parseFloat(newRecord.latitude));
                newLongitude = Math.abs(parseFloat(newRecord.longitude));

                if (Math.abs(lastLatitude-newLatitude) > 0.0005 || Math.abs(lastLongitude-newLongitude) > 0.0005){
                    newJsonToWrite = JSON.stringify(allRecords, null, 2);
                    fs.writeFile("records.json", newJsonToWrite, function(err){
                        if (err) throw err;
                            console.log('The "data to append" was appended to file!');
                        }
                    )
                }
            }
            //If the file is empty
            else {
                allRecords.records.push(newRecord);
                newJsonToWrite = JSON.stringify(allRecords, null, 2)
                fs.writeFile("records.json", newJsonToWrite, function(err){
                    if(err) throw err;
                })
            }
        }
    })

}
var testVariable;

module.exports = {saveNewRecord, testVariable}

