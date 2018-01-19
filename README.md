# Food info barcode scanner
This app scans a barcode using Google Play Services Motion Vision API and looks up the nutrient info and ingredients
of the corresponding food from the USDA NDB API. It can parse the ingredients and decide if it is peanut, gluten, etc. free.

This app allows your camera to scan a barcode automatically without taking a picture, or uploading a barcode.
It also uses autofocus and high resoltuion to scan barcodes from any reasonable distance, and can scan a barcode
in any orientation.

The main issue with this application is that the USDA NDB API doesnt have enough UPCs to be considered comprehensive
in any manner. So it may only recognize half of the foods in your kitchen.
