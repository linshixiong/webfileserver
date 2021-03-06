﻿// Put your custom code here

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
		console.log("pageInit");

		
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
			showToast("无法在当前路径新建资料夹");
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
		$("#fileNameDelete").html("档案名称："+fileSelectToDelete);
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
		console.log("delete :"+fileName);
		xhr.send("delete="+fileName);
		var dialog=$("#deleteFileDialog");
		dialog.popup( "close");
	
	}
	
	 function deleteComplete(evt) {
		refreshList(evt.target.responseText);
	 }
	
	function FileitemClick(obj){
			
			console.log("filelistClick");
	
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
			console.log("downloadFile data_icon="+data_icon);
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
			console.log("请选择需要下载的文件.");
			showToast("请选择需要下载的文件.");
		}
		else{
			console.log("设置下载链接...");
			var fileName=$(selected).find("img").attr("alt");
			var fileSize=$(selected).attr("fileSize");
			downloadFileName=fileName;
			$("#downloadFileName").html("文件名："+fileName);
			$("#downloadFileSize").html("文件大小："+getFileSize(fileSize));
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
		console.log("select file upload to "+uri);
		if(uri==""||uri=="/")
		{
			showToast("无法在当前路径上传档案");
			return;
		}
		
		$("#file").change(function () {
			
            var file = document.getElementById('file').files[0];
			if (file) {
				var fileSize = getFileSize(file.size);

				$("#uploadDialog").popup("open");
			}
			document.getElementById('fileName').innerHTML = '名称: ' + file.name;
			document.getElementById('fileSize').innerHTML = '大小: ' + fileSize;

        });
		$( "#uploadDialog" ).on( "popupafterclose", function( event, ui ) {	
			console.log("uploadDialog close");
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
		console.log("mkdir :"+dirName);
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
		  console.log("uploadStart");
		   var target=document.getElementById("uploadProgress"); 
           target.style.display="block";
            
	  }
	  
      function uploadProgress(evt) {
        if (evt.lengthComputable) {
          var percentComplete = Math.round(evt.loaded * 100 / evt.total);
          document.getElementById('progressNumber').innerHTML = "正在上传，完成"+ percentComplete.toString() + '%';
        }
        else {
          document.getElementById('progressNumber').innerHTML = '正在上传...';
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
		//alert(evt.target.responseText);
      }

	
	function openFile(){
		var selected=getSelectedFile();
		if(selected==undefined){
			showToast("请选择需要打开的文件.");
		}
		
		else{
			var fileName=$(selected).find("img").attr("alt");
			if(fileName.lastIndexOf(".")>0){
				var extName=fileName.substring(fileName.lastIndexOf(".")+1,fileName.length).toLowerCase();
				console.log("file extension name is "+extName);
				
				//打开图片预览
				if(extName=="jpg"||extName=="jpeg"||extName=="bmp"||extName=="png"||extName=="gif"){
				
					console.log("view image file "+fileName);
					window.open (fileName+'?action=play') ;
				}
				else if(extName=="mp3"||extName=="ogg"||extName=="wav"){
					console.log("play audio "+fileName);
					
					window.open (fileName+'?action=play') ;

				}else if (extName=="mp4"){
					console.log("play video "+fileName);
					window.open (fileName+'?action=play') ;
					
				}else
				{
					showToast("此文件类型无法直接打开，请下载此文件!");
					return;
				}
				
			}else{
				showToast("此文件类型无法直接打开，请下载此文件!");		
				return;
			}

		
		}
	}


		
	