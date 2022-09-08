# Mobile_System_Signal_Mapping
This repository contains all the files needed for the Mobile Systems' project of Ivan Kostine. 

## Introduction
This SignalMappingApp allows users from the whole world to contribute to a centralized database by uploading information concerning the data network at the location they are at.


## Installation
Please follow these steps for installation:

1. Open the /frontend folder in Android studio.
2. Open the /backend folder in any IDE and execute the following command to install all the NodeJS dependencies:
`npm install`
3. To start the server, go to the /backend folder and execute the following command:
`npm start`
4. Install the latest verion of Ngrok from [here](https://ngrok.com/)
5. After installing Ngrok, execute the following command:
`ngrok http 3000`
6. Copy the link generated by ngrok and update the existing link in the two following files in Android studio: SaveCoordinatesService.java (line 33) and MapsActivity.java (line 46)


## Running the application on any Android device or Emulator:
1. Make sure the backend server is running. If not, execute the following command `npm start` from the backend folder.
2. Go to the frontend folder in Android studio and run the project.
