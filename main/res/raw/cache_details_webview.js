if (document.getElementById('hlSignIn') == null) {
    var contents = document.getElementsByClassName('UserSuppliedContent');
	
    var container = document.createElement('div');
    container.style.backgroundColor = '#fff';

    while (contents.length > 0) {
        container.appendChild(contents[0]);
    }
	
    while (document.body.lastChild) {
        document.body.removeChild(document.body.lastChild);
    }
    document.body.appendChild(container);
}