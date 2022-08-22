const fs = require('fs');
const { stringify } = require('querystring');

function getAllRecords(){
    var allRecords;

    data = fs.readFileSync("./records.json")
    if (data != ""){
        allRecords = JSON.parse(data)
        return allRecords;
    }
    else{
        return {"error":"no records found"}
    }
}


module.exports = {getAllRecords};