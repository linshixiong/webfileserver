// Put your custom code here

	var isDownloadClicked;
	var mkdirOK;
	var downloadFileName;
	
	function showToast(msg){
		$("#toast_msg").html(msg);
		$("#toast" ).popup( "option", "transition", "pop" );
		$("#toast" ).popup( "open", "" );
		$( "#toast" ).on( "popupafteropen", function( event, ui ) {	
			window.setTimeout(cloeseToast,800);		
		} );
	}
		
	function cloeseToast(){
		$("#toast").popup( "close" );
	}
	
	function pageInit (event){
		//console.log("pageInit");

		
		$( "#downloadDialog" ).on( "popupafterclose", function( event, ui ) {	
			console.log("downloadDialog popupafterclose");
			$( "#downloadDialog" ).popup( "close");
		});
	}
	
	function refreshList(html){
		$("#filelist").html(html);
		$("#filelist").listview('refresh'); 
		$("#delete_button").hide();
	}
	
		function pageShow(event,ui){
			console.log("page show");
			$(ui.prevPage).remove();
			
		}
	
	function showMkdirDialog(){
		var uri= window.location.pathname;
		if(uri==""||uri=="/")
		{
			showToast(string_canot_mkdir);
			return;
		}
		var dialog=$("#mkdirDialog");
		dialog.popup( "option", "transition", "pop" );
		dialog.popup( "open", "" );
	}
	
	var fileSelectToDelete;
	function showDeleteConfirm(){
		var selected=getSelectedFile();
		fileSelectToDelete=$(selected).find("img").attr("alt");
		$("#fileNameDelete").html(string_file_name+fileSelectToDelete);
		var dialog=$("#deleteFileDialog");
		dialog.popup( "option", "transition", "pop" );
		dialog.popup( "open" );
		
	}
	
	function deleteFile(){
		var fileName=fileSelectToDelete;
		var xhr = new XMLHttpRequest();
 
		xhr.addEventListener("load", deleteComplete, false);
		xhr.addEventListener("error", deleteComplete, false);
		xhr.open("POST", ".");
		xhr.setRequestHeader("Content-Type","application/x-www-form-urlencoded"); 
		//console.log("delete :"+fileName);
		xhr.send("delete="+fileName);
		var dialog=$("#deleteFileDialog");
		dialog.popup( "close");
	
	}
	
	 function deleteComplete(evt) {
		refreshList(evt.target.responseText);
	 }
	
	function FileitemClick(obj){
			
			//console.log("filelistClick");
	
			 var length= $("#filelist > li").length;
			 for(var i=0;i<length;i++)
			 {
				var o= $("#filelist > li").get(i);
				
				if($(o).attr("isDirectory")=="true")
				{
					continue;
				}
				
				$(o).buttonMarkup({ icon: "file" });
				$(o).find("a").removeClass("checked"); 	
				
			 }
			if($(obj).attr("isDirectory")!="true"){
				$(obj).buttonMarkup({ icon: "file-checked" });
				$(obj).addClass("checked"); 
				$(obj).find("a").addClass("checked"); 
				$("#delete_button").show();
			}else{
				$("#delete_button").hide();
			}
	}
	
	function getSelectedFile(){
	
		var length= $("#filelist > li").length;
		var selected; 
		for(var i=0;i<length;i++)
		{
			var o= $("#filelist > li").get(i);
			if($(o).attr("isDirectory")=="true")
			{
				continue;
			}
			var data_icon= $(o).attr("data-icon");
			//console.log("downloadFile data_icon="+data_icon);
			if(data_icon=="file-checked"){
				selected=o;
				break;
			}		
		}
		return selected;
	}
	
  	function downloadFile()
	{		
		var selected=getSelectedFile();
		if(selected==undefined){
			showToast(string_select_file_to_download);
		}
		else{
			
			var fileName=$(selected).find("img").attr("alt");
			var fileSize=$(selected).attr("fileSize");
			downloadFileName=fileName;
			$("#downloadFileName").html(string_file_name+fileName);
			$("#downloadFileSize").html(string_file_size+getFileSize(fileSize));
			$("#download_file_url").attr("href",fileName);
			$("#download_file_url").attr("download",fileName);
			
			$( "#downloadDialog" ).popup( "option", "transition", "pop" );
			$( "#downloadDialog" ).popup( "open", "" );

		}
			
	}
	
	
	function startDownloadFile()
	{
		window.setTimeout(closeDownloadDialog,100);	
	}
	
	function closeDownloadDialog()
	{	
		$("#downloadDialog" ).popup( "close");
	}
	
	function getFileSize(length)
    {
    	if(length<0){
			return "unknown";
		}
    	if(length<1024){
    		return length+ "B";
    	}
    	length=length/1024;
    	if(length<1024){
    		return  toDecimal(length)+ "KB";
    	}
    	length=length/1024;
    	if(length<1024){
    		return  toDecimal(length)+ "MB";
    	}
    	length=length/1024;
    
    	return  toDecimal(length)+ "GB";
    	
    }
	
	function toDecimal(x) {    
        var f = parseFloat(x);    
        if (isNaN(f)) {    
            return;    
        }    
        f = Math.round(x*100)/100;    
        return f;    
    }    
	
	function selectFile()
	{
		var uri= window.location.pathname;
		if(uri==""||uri=="/")
		{
			showToast(string_canot_upload);
			return;
		}
		
		$("#file").change(function () {
			
            var file = document.getElementById('file').files[0];
			if (file) {
				var fileSize = getFileSize(file.size);

				$("#uploadDialog").popup("open");
			}
			document.getElementById('fileName').innerHTML = string_file_name + file.name;
			document.getElementById('fileSize').innerHTML = string_file_size + fileSize;

        });
		$( "#uploadDialog" ).on( "popupafterclose", function( event, ui ) {	
			if(xhr!=undefined){
				xhr.abort();
			}
			var target=document.getElementById("uploadProgress");
			target.style.display="none";
			$("#file").val("");	
		} );
		$("#file").click();  
	}

	function showDownloadDialog()
	{
		$("#downloadDialog").popup("open");
	}
	
	function  mkdir(){
		var dirName=$('#dir_name').val();
		var xhr = new XMLHttpRequest();
		xhr.addEventListener("load", mkdirComplete, false);
		xhr.addEventListener("error", mkdirComplete, false);
		xhr.open("POST", ".");
		xhr.setRequestHeader("Content-Type","application/x-www-form-urlencoded");  
		xhr.send("mkdir="+dirName);
		$("#mkdirDialog").popup("close");
	}	
		
	 function mkdirComplete(evt) {
		refreshList(evt.target.responseText);
	 }

	var xhr;
	function uploadFile() {
        var fd = new FormData();
        fd.append("file", document.getElementById('file').files[0]);
        xhr = new XMLHttpRequest();
        xhr.upload.addEventListener("progress", uploadProgress, false);
		xhr.upload.addEventListener("loadstart", uploadStart, false);
        xhr.addEventListener("load", uploadComplete, false);
        xhr.addEventListener("error", uploadComplete, false);
        xhr.addEventListener("abort", uploadCanceled, false);
        xhr.open("POST", ".");
        xhr.send(fd);
      }
	  function uploadStart(evt){
		   var target=document.getElementById("uploadProgress"); 
           target.style.display="block";
            
	  }
	  
      function uploadProgress(evt) {
        if (evt.lengthComputable) {
          var percentComplete = Math.round(evt.loaded * 100 / evt.total);
          document.getElementById('progressNumber').innerHTML = string_uploading_with_progress+ percentComplete.toString() + '%';
        }
        else {
          document.getElementById('progressNumber').innerHTML = string_uploading;
        }
      }
      function uploadComplete(evt) {
		$("#uploadDialog").popup("close");
		refreshList(evt.target.responseText);
		pageInit(this);
      }
      function uploadFailed(evt) {
		$("#uploadDialog").popup("close");
	  }
      function uploadCanceled(evt) {
        $("#uploadDialog").popup("close");
      }

	
	function openFile(){
		var selected=getSelectedFile();
		if(selected==undefined){
			showToast(string_select_file_to_open);
		}
		
		else{
			var fileName=$(selected).find("img").attr("alt");
			if(fileName.lastIndexOf(".")>0){
				var extName=fileName.substring(fileName.lastIndexOf(".")+1,fileName.length).toLowerCase();

				if(extName=="jpg"||extName=="jpeg"||extName=="bmp"||extName=="png"||extName=="gif"){
					window.open (fileName+'?action=play') ;
				}
					else if(extName=="mp3"||extName=="ogg"||extName=="wav"){				
					window.open (fileName+'?action=play') ;
				}else if (extName=="mp4"){
					window.open (fileName+'?action=play') ;
				}else if(extName=="txt"||extName=="pdf"){
					window.open (fileName+'?action=play') ;
				}
				else
				{
					showToast(string_cannot_open_file);
					return;
				}
				
			}else{
				showToast(string_cannot_open_file);		
				return;
			}

		
		}
	}


		
	