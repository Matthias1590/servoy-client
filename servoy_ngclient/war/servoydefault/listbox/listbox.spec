{
	"name": "servoydefault-listbox",
	"displayName": "ListBox",
	"version": 1,
	"icon": "servoydefault/listbox/listbox.png",
	"definition": "servoydefault/listbox/listbox.js",
	"libraries": [{"name":"svy-listbox", "version":"1", "url":"servoydefault/listbox/listbox.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}, "displayTagsPropertyName" : "displaysTags"}, 
	        "displaysTags" : { "type" : "boolean", "tags": { "scope" : "design" } }, 
	        "editable" : { "type": "protected", "blockingOn": false, "default": true,"for": ["dataProviderID","onDataChangeMethodID","onFocusGainedMethodID","onFocusLostMethodID"] }, 
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID","onFocusGainedMethodID","onFocusLostMethodID","onRightClickMethodID"] }, 
	        "findmode" : { "type":"findmode", "tags":{"scope":"private"}, "for" : {"editable":true}}, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "format" : {"for":["valuelistID","dataProviderID"] , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "tags": { "scope" :"design" }}, 
	        "multiselectListbox" : "boolean", 
	        "placeholderText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "readOnly" : { "type" : "readOnly", "oppositeOf" : "editable"}, 
	        "scrollbars" : {"type" :"scrollbars", "tags": { "scope" :"design" }}, 
	        "size" : {"type" :"dimension",  "default" : {"width":140, "height":140}}, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :["form-control", "input-sm", "svy-padding-xs"]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "text" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "transparent" : "boolean", 
	        "valuelistID" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataProviderID"}, 
	        "visible" : "visible" 
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDataChangeMethodID" : "function", 
	        "onFocusGainedMethodID" : "function", 
	        "onFocusLostMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
	        "getHeight": {
	            "returns": "int"
	        },
	        "getLocationX": {
	            "returns": "int"
	        },
	        "getLocationY": {
	            "returns": "int"
	        },
	        "getScrollX": {
	            "returns": "int"
	        },
	        "getScrollY": {
	            "returns": "int"
	        },
	        "getSelectedElements": {
	            "returns": "object []"
	        },
	        "getWidth": {
	            "returns": "int"
	        },
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	        },
	        "setScroll": {
				"parameters":[
								{                                                                 
 								"name":"x",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"y",
								"type":"int"
			                	}             
							 ]
	        }
	}
	 
}