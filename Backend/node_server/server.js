var saveRecords = require("./controllers/saveLocations");
var getLocations = require("./controllers/getLocations");

const bodyParser = require("body-parser");

const express = require('express')
const app = express()
app.use(bodyParser.json())
const port = 3000

app.get('/records', (req, res) =>{
  //console.log(getRecords.getAllRecords())
  console.log(getLocations.getAllRecords())
  res.send(getLocations.getAllRecords())

})

app.post('/records', (req, res) => {
  //console.log(req.body)
  saveRecords.saveNewRecord(req.body)
  res.send({"Response": "Record saved successfully"})

})

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`)
})