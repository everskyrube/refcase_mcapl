let gridworld = null;
let robot = null;

var canvas = null;
let ws = null;
function draw() {
    canvas = document.getElementById("gridworld");

    if (canvas.getContext) {
        const ctx = canvas.getContext("2d");
        let cwidth=500;
        let cheight=500;
        let rows = 10;
        let cols = 10;

        gridworld = new GridWorld(ctx,rows,cols,cwidth,cheight);
        gridworld.drawGrid(canvas)
        robot = new Robot(ctx,5,"darkslateblue");
        move_robot(0,0);

    }
    document.getElementById("movebutton").addEventListener("click",move);
    document.getElementById("startbutton").addEventListener("click",start);
}
function dows() {
    ws.onopen = function () {
        document.getElementById("wsmsg").innerText = "Opened connection to websocket";
        console.info("opened connection to websocket");
    }
    ws.onmessage = function (evt) {
        document.getElementById("wsmsg").innerText = "Message: " + evt.data;
        console.info("Got a message");
    }
    ws.onclose = function () {
        document.getElementById("wsmsg").innerText = "<br>WebSocket closed";
    };

    ws.onerror = function (err) {
        document.getElementById("wsmsg").innerText = "Error: " + err;
    };
}
function start()
{
    if(ws!=null)
    {
        ws.close();
    }
    try {
        ws = new WebSocket("ws://localhost:8080/gridworld/example");
        dows();
        if (ws.readyState == 1) {
            ws.send("started");
            console.info("sent to ws ", "started")
        }
    }
    catch (e) {
        console.log("Unable to connect to websocket")
        console.log(e);
    }
}
function move() {
    // just redraw the robot in a different location
    // get the robot's grid val
    // just move it to the next val
    let rr = robot.row;
    let rc = robot.col;
    if(rr<gridworld.numr-1)
    {
        rr++;
    }
    else{
        rr = 0;
        if(rc<gridworld.numc-1)
        rc++;
        else
            rc=0;
    }

    gridworld.clearGrid();
    gridworld.restoreGrid();
    move_robot(rr,rc);
    console.info("Moving to ",rr,rc);

}
function move_robot(rr,rc)
{
    let robot_xy = gridworld.get_cell_center(rr,rc);
    robot.set_grid_val(rr,rc);
    robot.draw(robot_xy[0],robot_xy[1]);
    if(ws!=null && ws.readyState==1) {
        let moved_literal = `movebase_result(${rr}_${rc},3)`;
        ws.send(moved_literal);
        console.info("Sent to ws ", moved_literal);
    }
}
class Robot{
    constructor(ctx,radius,color) {
        this.ctx = ctx;
        this.radius = radius;
        this.color = color;
        this.x = null;
        this.y = null;

    }
    draw(x0,y0)
    {
        let ctx = this.ctx;
        let radius = this.radius;

        ctx.fillStyle=this.color
        ctx.beginPath();
        ctx.arc(x0,y0,radius,0,3.142*2,true);
        ctx.fill();
        ctx.fillStyle="black";
        this.x = x0;
        this.y = y0;
    }
    set_grid_val(row,col)
    {
        this.row = row;
        this.col = col;
    }

}
class GridWorld{
    constructor(ctx,numr,numc,w,h) {
        this.ctx = ctx;
        this.numr = numr;
        this.numc = numc;
        this.wpx = w/numr;
        this.hpx = h/numc;
        this.gridworldcanvasstate =   null;
        this.clearw = null;
        this.clearh = null;

    }

    drawGrid(canvas)
    {
        let ctx = this.ctx;
        let wpx = this.wpx;
        let hpx = this.hpx;
        let numr = this.numr;
        let numc = this.numc;
        let x0 = 0;
        let y0 = 0;

        while(x0!=numr*hpx && y0!=numc*wpx) {
            ctx.strokeRect(x0, y0, wpx, hpx);
            x0 += wpx;
            if (x0 == numc * wpx)
            {
                if(y0 != numr * hpx)
                {
                    x0 = 0;
                    y0 +=hpx;
                }
            }
        }
        //write the col and row numbers

        let label_row_start_x = numc*wpx + wpx/4;
        let label_row_start_y = hpx/2;
        for(let i = 0; i<numr; i++) {
            ctx.fillText(`${i}`, label_row_start_x, label_row_start_y);
            label_row_start_y+=hpx;
        }
        label_row_start_y+=(hpx/4);
        for(let i = numc-1; i>=0; i--)
        {
            label_row_start_x-=wpx;
            ctx.fillText(`${i}`,label_row_start_x,label_row_start_y);
        }
        this.gridworldcanvasstate =   ctx.getImageData(0,0,canvas.width,canvas.height);
        this.clearw = canvas.width;
        this.clearh = canvas.height;

    }
    clearGrid()
    {

        this.ctx.clearRect(0,0,this.clearw,this.clearh);
    }
    restoreGrid()
    {
        if (this.gridworldcanvasstate !=null)
        this.ctx.putImageData(this.gridworldcanvasstate,0,0);
    }
    get_cell_center(row_val,col_val)
    {
        //we start at 0 and 0
        // calculate cell start x and y
        let cell_x = row_val*this.wpx;
        let cell_y = col_val*this.hpx;
        // cell center
        cell_x = cell_x + this.wpx/2;
        cell_y = cell_y + this.hpx/2;

        return [cell_x,cell_y]

    }
}
