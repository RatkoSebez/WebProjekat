$(document).ready(function(){
    var users;
    $.get({
        url: "rest/userService/getUsers",
        success: function(data){
            users = data;
            buildTable(data);
        }
    });
    function buildTable(data){
        $("#users > tbody").html("");
        for(let users of data){
                let tbody = $('#users tbody');
                let name = $('<td>' + users.name + '</td>');
                let surname = $('<td>' + users.surname + '</td>');
                let username = $('<td>' + users.username + '</td>');
                let gender = $('<td>' + users.gender + '</td>');
                let role = $('<td>' + users.role + '</td>');
                let discountPoints = $('<td>' + users.discountPoints + '</td>');
                //datum rodjenja
                let date = new Date(users.birthDate);
                var day = date.getDate();
                var month =  date.getMonth() + 1;
                var year = date.getFullYear();
                formattedDate = day + "." + month + "." + year + ".";
                let birthDate = ($('<td>' + formattedDate + '</td>'));
                //kreiranje tabele
                let tr = $('<tr></tr>');
                tr.append(name).append(surname).append(username).append(role).append(gender).append(birthDate).append(discountPoints);
                tbody.append(tr);
            }
    }

    $('#roleSelect').on('change', function() {
        filterTable();
    });
    $('#customerTypeSelect').on('change', function() {
        var filteredData = searchTableSelectCustomerType(this.value);
        buildTable(filteredData);
    });
    $("#searchName").on('keyup', function(){
        filterTable();
    });
    $("#searchSurname").on('keyup', function(){
        filterTable();
    });
    $("#searchUsername").on('keyup', function(){
        filterTable();
    });

    $(".sortColumn").mouseover(function() {
        $(this).css('cursor', 'pointer'); 
    });

    //sortiranje
    $('.sortColumn').on('click', function(){
        var column = $(this).data('column');
        var order = $(this).data('order');
        var text = $(this).html();
        text = text.substring(0, text.length-1);
        //console.log(column + " " + order);
        if(order == 'desc'){
            $(this).data('order', 'asc');
            users = users.sort((a,b) => a[column] > b[column] ? 1 : -1);
            text += '&#9660';
        }
        else{
            $(this).data('order', 'desc');
            users = users.sort((a,b) => a[column] < b[column] ? 1 : -1);
            text += '&#9650';
        }
        $(this).html(text);
        buildTable(users);
    });

    function filterTable(){
        var roleInput = $('#roleSelect').find(":selected").text().toLowerCase();
        var customerTypeInput = $('#customerTypeSelect').find(":selected").text().toLowerCase();
        var nameInput = $('#searchName').val().toLowerCase();
        var surnameInput = $('#searchSurname').val().toLowerCase();
        var usernameInput = $('#searchUsername').val().toLowerCase();
        var allUsers = users;
        var tmpUsers = [];
        //name
        console.log(nameInput + ", " + surnameInput);
        for(var i=0; i<allUsers.length; i++){
            var name = allUsers[i].name.toLowerCase();
            if(name.includes(nameInput)) tmpUsers.push(allUsers[i]);
        }
        allUsers = tmpUsers;
        tmpUsers = [];
        //surname
        for(var i=0; i<allUsers.length; i++){
            var surname = allUsers[i].surname.toLowerCase();
            if(surname.includes(surnameInput)) tmpUsers.push(allUsers[i]);
        }
        allUsers = tmpUsers;
        tmpUsers = [];
        //username
        for(var i=0; i<allUsers.length; i++){
            var username = allUsers[i].username.toLowerCase();
            if(username.includes(usernameInput)) tmpUsers.push(allUsers[i]);
        }
        allUsers = tmpUsers;
        tmpUsers = [];
        //role
        console.log(roleInput);
        if(roleInput == 'role') tmpUsers = allUsers;
        for(var i=0; i<allUsers.length; i++){
            if(roleInput == 'role') break;
            var role = allUsers[i].role.toLowerCase();
            if(role == roleInput) tmpUsers.push(allUsers[i]);
        }
        allUsers = tmpUsers;
        currentUsers = allUsers;
        buildTable(allUsers);
    }

    function searchTableSelectCustomerType(input){
        var searchedData = [];
        if(input == "All") return users;
        input = input.toLowerCase();
        var customerType;
        for(var i=0; i<users.length; i++){
            $.post({
                url: "rest/userService/getCustomerType",
                data: JSON.stringify(users[i].username),
                contentType: "application/json",
                success: function(data2){
                    cutomerType = data2;
                },
            });
            //alert(customerType);
            if(customerType == input){
                searchedData.push(users[i]);
            }
        }
        return searchedData;
    }

    $("#reset").click(function(){
        $('#roleSelect').val('All').change();
        $('#customerTypeSelect').val('All').change();
        $('#searchName').val('');
        $('#searchSurname').val('');
        $('#searchUsername').val('');
    });

});