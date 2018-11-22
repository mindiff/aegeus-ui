(function($) {
    $(document).on('click', '.far.fa-copy', function(e) {
        var address = $(this).prev('.copy-address');
        $(address).selectText();
        document.execCommand('copy');
        document.getSelection().removeAllRanges();
        $(address).fadeOut(500);
        $(address).fadeIn(500);
    });

    $(document).on('click', '.assign-btn', function(e) {
        e.preventDefault();
        var address = $(this).data('address'),
            label   = $('#addr-label-' + address).val();

        $('#assign-label-' + address).val( label );

        $('#form-assign-label-' + address).submit();
    });

    $(document).on('submit', '#add-address', function(e) {
        var label = $('#new-address-name').val();

        if ( ! label || label === '' ) {
            e.preventDefault();
            $('#new-address-name').addClass('is-invalid');
            return false;
        }

        return true;
    });

    $(document).on('submit', '#import-address', function(e) {
        var label = $('#import-label').val(),
            key = $('#import-key').val();

        if ( ! label || label === '' ) {
            e.preventDefault();
            $('#import-label').addClass('is-invalid');
            return false;
        }

        if ( ! key || key === '' ) {
            e.preventDefault();
            $('#import-key').addClass('is-invalid');
            return false;
        }

        return true;
    });
})(jQuery);
