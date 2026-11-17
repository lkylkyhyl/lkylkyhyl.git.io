# Same operations as Java, using separate lab3_shell tables.
%w[lab3_shell_Student lab3_shell_Course lab3_shell_SC lab3_shell_ops].each do |t|
  if exists(t)
    disable t
    drop t
  end
end
create 'lab3_shell_Student', 'Info', 'Score'
create 'lab3_shell_Course', 'Info'
create 'lab3_shell_SC', 'Info'
students = [['2015001','Zhangsan','male','23'],['2015002','Mary','female','22'],['2015003','Lisi','male','24']]
courses = [['123001','Math','2.0'],['123002','Computer Science','5.0'],['123003','English','3.0']]
choices = [['2015001','123001','86'],['2015001','123003','69'],['2015002','123002','77'],['2015002','123003','99'],['2015003','123001','98'],['2015003','123002','95']]
students.each do |s|
  %w[S_No S_Name S_Sex S_Age].each_with_index { |f,i| put 'lab3_shell_Student', s[1], "Info:#{f}", s[i] }
end
courses.each do |c|
  %w[C_No C_Name C_Credit].each_with_index { |f,i| put 'lab3_shell_Course', c[0], "Info:#{f}", c[i] }
end
choices.each do |s|
  %w[SC_Sno SC_Cno SC_Score].each_with_index { |f,i| put 'lab3_shell_SC', "#{s[0]}_#{s[1]}", "Info:#{f}", s[i] }
  name = students.find { |a| a[0] == s[0] }[1]
  course = courses.find { |a| a[0] == s[1] }[1]
  put 'lab3_shell_Student', name, "Score:#{course}", s[2]
end
list 'lab3_.*'
describe 'lab3_shell_Student'
scan 'lab3_shell_Student'
scan 'lab3_shell_Course'
scan 'lab3_shell_SC'
count 'lab3_shell_Student'
count 'lab3_shell_Course'
count 'lab3_shell_SC'
scan 'lab3_shell_Student', { COLUMNS => 'Score' }
scan 'lab3_shell_Student', { COLUMNS => 'Score:Math' }
# Shell filtered scans omit missing rows. get explicitly demonstrates null.
get 'lab3_shell_Student', 'Mary', 'Score:Math'
put 'lab3_shell_Student', 'Zhangsan', 'Score:Math', '90'
get 'lab3_shell_Student', 'Zhangsan', 'Score:Math'
put 'lab3_shell_Student', 'Zhangsan', 'Score:Math', '86'
create 'lab3_shell_ops', 'Info'
alter 'lab3_shell_ops', NAME => 'Extra'
put 'lab3_shell_ops', 'temp', 'Info:x', '1'
put 'lab3_shell_ops', 'temp', 'Extra:y', '2'
deleteall 'lab3_shell_ops', 'temp', 'Extra:y'
alter 'lab3_shell_ops', NAME => 'Extra', METHOD => 'delete'
deleteall 'lab3_shell_ops', 'temp'
count 'lab3_shell_ops'
put 'lab3_shell_ops', 'temp', 'Info:x', '1'
truncate 'lab3_shell_ops'
count 'lab3_shell_ops'
puts 'HBASE_SHELL_COMPLETE'
exit
