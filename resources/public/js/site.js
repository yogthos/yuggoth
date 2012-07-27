$(document).ready(function(){    
    if ($("#post-preview").length != 0) renderPreview();
    $($("#selected").val()).toggleClass("selected");
  	 
  	$("#content").keypress(renderPreview);  	
  	$(".submit").keypress(onEnter);
  	$(".submit").click(function(){$(this).parents("FORM").submit();});  	
    $(".delete").click(function(e){
    jConfirm('', 
             'Confirm delete?', 
             function(r) {
               // If they confirmed, manually trigger a form submission
               if (r) $(".delete").parents("FORM").submit();
             });
    // Always return false here since we don't know what jConfirm is going to do
    return false;
  });
 	
});

function renderPreview() {
	var md = $("#content").val();
	$("#post-preview").html(markdown.mdToHtml(md));   	
}

function onEnter(e)
{
	var keynum;
	if(window.event) // IE8 and earlier
		keynum = e.keyCode;	
	else if(e.which) // IE9/Firefox/Chrome/Opera/Safari	
		keynum = e.which;		
	if(keynum === 13) $(this).trigger("click");
}