db.users.insertMany([
    {
        id: 'testUser1',
        mail: 'testone@example.com',
        payPalMail: "sb-mqfsr1408810@personal.example.com",
        name: 'Test One',
        date: Date()
    },
    {
        id: 'testUser2',
        mail: 'testtwo@example.com',
        payPalMail: "sb-llztk635693@business.example.com",
        name: 'Test Two',
        date: Date()
    },
    {
        id: 'testUser3',
        mail: 'testthree@example.com',
        payPalMail: null,
        name: 'Test Three',
        date: Date()
    },
    {
        id: 'testUser4',
        mail: 'testfour@example.com',
        payPalMail: null,
        name: 'Test Four',
        date: Date()
    },
    {
        id: 'testUser5',
        mail: 'testfive@example.com',
        payPalMail: null,
        name: 'Test Five',
        date: Date()
    }
])
