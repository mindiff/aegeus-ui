(function($) {
    // on load of the page: switch to the currently selected tab
    var hash = window.location.hash;
    $('#pills-tab a[href="' + hash + '"]').tab('show');

    // Read a page's GET URL variables and return them as an associative array.
    function getUrlVars() {
        var vars = [], hash, site_hash = window.location.hash, arr_val;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for(var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split('=');

            if (site_hash) {
                arr_val = hash[1].replace(site_hash, '');
            } else {
                arr_val = hash[1];
            }

            vars.push(hash[0]);
            vars[hash[0]] = arr_val;
        }
        return vars;
    }

    $('.show-select-address').on('click', function(e) {
        e.preventDefault();
        $('#exampleModal').modal('toggle');
    });

    $(document).ready(function() {
        var vars = getUrlVars()

        $("#pills-tab > li > a").on("shown.bs.tab", function(e) {
            var id = $(e.target).attr("href").substr(1);
            window.location.hash = id;
        });

        $('.address').text(vars.addr).show();

    });

    $(document).on('click', '.show-file', function(e) {
        e.preventDefault();

        var address = $(this).data('address');
        var path = $(this).data('path');

        window.open( '/portal/fshow?addr=' + address + '&path=' + path, '_blank' );
    });

    $(document).on('submit', '#add-content-url', function(e) {
        var url = $('#content-url').val();

        console.log( 'URL: ' + url );
        console.log( isUrlValid(url) );
        if ( ! isUrlValid(url) ) {
            e.preventDefault();
            alert('Invalid URL');
        }
    });

    function isUrlValid(url) {
        regexp =  /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
        if (regexp.test(url)) {
            return true;
        }
        return false;
    }
})(jQuery);
