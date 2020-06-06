from flask import Flask, request, send_file, send_from_directory, url_for, Response
from os import listdir, getcwd
from os.path import isdir, isfile, abspath
import jinja2 as jin
import json


root_folder = getcwd() + "/"
app = Flask(__name__, root_path=root_folder, static_url_path=root_folder)

def generate_html(html_template_path: str, input_values: dict):
    html_temp = read_file(html_template_path)
    jin_temp = jin.Template(html_temp)
    html_rend = jin_temp.render(input_values)
    return html_rend


def read_file(file_path: str):
    with open(file_path, "rb") as file:
        return file.read()


@app.route("/", methods=['GET'])
def home_page():
    return read_file("register.html")


@app.route("/regForm", methods=['POST'])
def check_data():
    dat = json.loads(request.get_data().decode())
    print("remote_addr: {}".format(request.remote_addr))
    print(dat)
    return {"status": "ok"}


@app.route("/uploadFile", methods=['POST'])
def upload_file():
    file = request.files["billFile"]
    form = request.form.get("cycle_billing"), request.form.get("bill_type")
    print(file)
    print(form)
    return {"status": "ok"}

# def folder_info(path=""):
#     path = root_folder if path == "" else abspath(path) + "/"
#     curr_dir_list = listdir(path)
#     dir_list = list(filter(lambda f: isdir(path + f), curr_dir_list))
#     files_list = list(filter(lambda f: isfile(path + f), curr_dir_list))
#     dir_info = {"directory": path,
#                 "dir_list": dir_list,
#                 "files_list": files_list}
#     return generate_html("static/WebModules/folder_page.html", dir_info)

# @app.route("/assets/<fold>/<file>", methods=["GET"])
# def get_file(fold, file):
#     with app.open_resource("{}/{}/{}/{}".format(root_folder, "assets", fold, file)) as f:
#         return f.read().decode("utf-8")
#     #return app.open_resource(, mode="rb").read()

@app.route("/assets/<fold>/<file>", methods=["GET"])
def get_resource(fold, file):
    print("remote_addr: {}".format(request.remote_addr))
    mimetypes = {
        "css": "text/css",
        "html": "text/html",
        "js": "application/javascript",
    }
    cont = read_file("{}/{}/{}/{}".format(root_folder, "assets", fold, file))
    ext = file.split(".")[-1]
    if ext in mimetypes:
        mimetype = mimetypes[ext]
    else:
        mimetype = mimetypes["html"]
    return Response(cont, mimetype=mimetype)



@app.route("/images/<img>", methods=["GET"])
def get_img(img):
    return send_file("{}/{}/{}".format(root_folder, "images", img))

@app.route("/<html_file>.html", methods=["GET"])
def get_html_page(html_file):
    print("remote_addr: {}".format(request.remote_addr))
    return read_file("{}/{}.html".format(root_folder, html_file))



# @app.route("/folder_search", methods=["GET", "POST"])
# def get_folder_info():
#     folder_path = json.loads(request.data.decode("utf-8"))["folder_path"]
#     return folder_info(folder_path)
#
#
# @app.route("/file_content", methods=["GET", "POST"])
# def get_file_content():
#     file_path = json.loads(request.data.decode("utf-8"))["file_path"]
#     return read_file(file_path)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8055)
    # app.add_url_rule('css/<path:filename>', endpoint='css',
    #                  view_func=app.send_static_file)
    # url_for("/assets/js/", filename="{}/assets/js/".format(root_folder))
    # url_for('/images/', filename='/images/')
