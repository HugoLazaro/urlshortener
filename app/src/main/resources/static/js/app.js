$(document).ready(
    async function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: $(this).serialize(),
                    success: async function (msg, status, request) {
                        if (document.getElementById('getQR').checked) {
                            $("#result").html(
                                "<div class='alert alert-success lead'><a target='_blank' href='"
                                + request.getResponseHeader('Location')
                                + "'>"
                                + request.getResponseHeader('Location')
                                + "</a></div>"
                                + "<img width='350' src='"
                                + request.getResponseHeader('Location') + "/qr"
                                + "'>");
                        }
                        else{
                            $("#result").html(
                                "<div class='alert alert-success lead'><a target='_blank' href='"
                                + request.getResponseHeader('Location')
                                + "'>"
                                + request.getResponseHeader('Location')
                                + "</a></div>");
                        }

                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });

function doesFileExist(urlToFile) {
    var xhr = new XMLHttpRequest();
    xhr.open('HEAD', urlToFile, false);
    xhr.send();

    if (xhr.status == "404") {
        return false;
    } else {
        return true;
    }
}